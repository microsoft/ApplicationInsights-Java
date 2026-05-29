// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.testutil.TestTimeSource;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadListener;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadService;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import io.opentelemetry.contrib.jfr.connection.FlightRecorderConnection;
import io.opentelemetry.contrib.jfr.connection.Recording;
import io.opentelemetry.contrib.jfr.connection.RecordingConfiguration;
import io.opentelemetry.contrib.jfr.connection.RecordingOptions;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfilerGlobalCooldownTest {
  @TempDir File tempDir;

  private final TestTimeSource timeSource = new TestTimeSource();
  private ScheduledExecutorService executor;

  @AfterEach
  void tearDown() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  private static AlertBreach manualBreach(int durationSeconds) {
    return AlertBreach.builder()
        .setType(AlertMetricType.MANUAL)
        .setAlertValue(0.0)
        .setAlertConfiguration(
            AlertConfiguration.builder()
                .setType(AlertMetricType.MANUAL)
                .setEnabled(true)
                .setProfileDurationSeconds(durationSeconds)
                .build())
        .setProfileId(UUID.randomUUID().toString())
        .setCpuMetric(0)
        .setMemoryUsage(0)
        .build();
  }

  private Profiler createProfiler(int globalCooldownSeconds) {
    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.globalCooldownSeconds = globalCooldownSeconds;
    Profiler profiler =
        new Profiler(config, tempDir, timeSource) {
          @Override
          protected Recording createRecording(RecordingOptions opts, RecordingConfiguration cfg) {
            return mock(Recording.class);
          }
        };

    FlightRecorderConnection frc = mock(FlightRecorderConnection.class);
    when(frc.newRecording(any(), any())).thenReturn(mock(Recording.class));

    executor = Executors.newScheduledThreadPool(1);
    profiler.initialize(mock(UploadService.class), executor, frc);
    return profiler;
  }

  @Test
  void globalCooldownIsSetAfterRecordingCompletes() {
    Instant baseTime = Instant.parse("2025-01-01T00:00:00Z");
    timeSource.setNow(baseTime);

    Profiler profiler = createProfiler(120);
    UploadListener noOpListener = index -> {};
    profiler.profileAndUpload(manualBreach(1), Duration.ofSeconds(1), noOpListener);
    // Before clearing, cooldown should still be at MIN
    assertThat(profiler.getGlobalCooldownUntil()).isEqualTo(Instant.MIN);
    profiler.clearActiveRecording();
    // After clearing, cooldown should be exactly baseTime + 120s
    assertThat(profiler.getGlobalCooldownUntil()).isEqualTo(baseTime.plusSeconds(120));
  }

  @Test
  void globalCooldownNotSetWhenDisabled() {
    timeSource.setNow(Instant.parse("2025-01-01T00:00:00Z"));

    Profiler profiler = createProfiler(0);
    UploadListener noOpListener = index -> {};
    profiler.profileAndUpload(manualBreach(1), Duration.ofSeconds(1), noOpListener);
    profiler.clearActiveRecording();
    assertThat(profiler.getGlobalCooldownUntil()).isEqualTo(Instant.MIN);
  }

  @Test
  void secondProfileRejectedDuringCooldown() {
    Instant baseTime = Instant.parse("2025-01-01T00:00:00Z");
    timeSource.setNow(baseTime);

    Profiler profiler = createProfiler(600);
    UploadListener noOpListener = index -> {};
    // First profile starts and completes
    profiler.profileAndUpload(manualBreach(1), Duration.ofSeconds(1), noOpListener);
    profiler.clearActiveRecording();
    // Cooldown should now be active (baseTime + 600s)
    assertThat(profiler.getGlobalCooldownUntil()).isEqualTo(baseTime.plusSeconds(600));

    // Advance time but stay within cooldown window
    timeSource.setNow(baseTime.plusSeconds(300));

    // Second profile should be silently rejected (startRecording returns null due to cooldown)
    profiler.profileAndUpload(manualBreach(1), Duration.ofSeconds(1), noOpListener);
    // activeRecording should still be null (second profile was rejected)
    assertThat(profiler.isRecordingActive()).isFalse();
  }
}
