// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

class ProfilerContinuousProfilingTest {
  @TempDir File tempDir;

  private final TestTimeSource timeSource = new TestTimeSource();
  private ScheduledExecutorService executor;

  @AfterEach
  void tearDown() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  private static AlertBreach manualBreach(int profileDurationSeconds) {
    return AlertBreach.builder()
        .setType(AlertMetricType.MANUAL)
        .setAlertValue(0.0)
        .setAlertConfiguration(
            AlertConfiguration.builder()
                .setType(AlertMetricType.MANUAL)
                .setEnabled(true)
                .setProfileDurationSeconds(profileDurationSeconds)
                .build())
        .setProfileId(UUID.randomUUID().toString())
        .setCpuMetric(0)
        .setMemoryUsage(0)
        .build();
  }

  @Test
  void profileRequestAlwaysDumpsWholeBufferEvenForShorterRequestedDuration() throws Exception {
    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.enableContinuousProfiling = true;
    config.continuousProfilingMaxAgeSeconds = 60;
    config.globalCooldownSeconds = 0;

    Recording continuousRecording = mock(Recording.class);

    Profiler profiler =
        new Profiler(config, tempDir, timeSource) {
          @Override
          protected Recording createRecording(RecordingOptions o, RecordingConfiguration c) {
            return continuousRecording;
          }
        };

    UploadService uploadService = mock(UploadService.class);
    FlightRecorderConnection frc = mock(FlightRecorderConnection.class);
    executor = Executors.newScheduledThreadPool(1);
    profiler.initialize(uploadService, executor, frc);

    // Continuous recording is started up-front and kept running.
    verify(continuousRecording).start();

    Instant now = Instant.parse("2025-01-01T00:00:00Z");
    timeSource.setNow(now);
    UploadListener noOp = index -> {};

    // A live circular buffer can only be dumped in its entirety; a shorter portal-/JMX-configured
    // duration (10s) cannot be honored by streaming a sub-window from the still-running recording,
    // so the whole 60s buffer is dumped via the robust dump() path.
    profiler.profileAndUpload(manualBreach(10), Duration.ofSeconds(10), noOp);

    verify(continuousRecording).dump(anyString());
    verify(continuousRecording, never()).getStream(any(), any());
    verify(continuousRecording, never()).stop();
    // The captured window is the whole 60s buffer, so the profile is timestamped at now - 60s.
    verify(uploadService)
        .upload(any(), eq(now.minusSeconds(60).toEpochMilli()), any(File.class), any());
    assertThat(profiler.isRecordingActive()).isFalse();
  }

  @Test
  void profileRequestDumpsWholeBufferWhenRequestedDurationExceedsMaxAge() throws Exception {
    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.enableContinuousProfiling = true;
    config.continuousProfilingMaxAgeSeconds = 60;
    config.globalCooldownSeconds = 0;

    Recording continuousRecording = mock(Recording.class);
    Profiler profiler =
        new Profiler(config, tempDir, timeSource) {
          @Override
          protected Recording createRecording(RecordingOptions o, RecordingConfiguration c) {
            return continuousRecording;
          }
        };

    UploadService uploadService = mock(UploadService.class);
    FlightRecorderConnection frc = mock(FlightRecorderConnection.class);
    executor = Executors.newScheduledThreadPool(1);
    profiler.initialize(uploadService, executor, frc);

    verify(continuousRecording).start();

    timeSource.setNow(Instant.parse("2025-01-01T00:00:00Z"));
    Instant now = Instant.parse("2025-01-01T00:00:00Z");
    UploadListener noOp = index -> {};

    // The requested duration (90s) exceeds the 60s buffer, so the whole circular buffer is dumped
    // via the more robust dump() path.
    profiler.profileAndUpload(manualBreach(90), Duration.ofSeconds(90), noOp);

    verify(continuousRecording).dump(anyString());
    verify(continuousRecording, never()).getStream(any(), any());
    verify(continuousRecording, never()).stop();
    // The captured window is the whole 60s buffer, so the profile is timestamped at now - 60s.
    verify(uploadService)
        .upload(any(), eq(now.minusSeconds(60).toEpochMilli()), any(File.class), any());
    assertThat(profiler.isRecordingActive()).isFalse();
  }
}
