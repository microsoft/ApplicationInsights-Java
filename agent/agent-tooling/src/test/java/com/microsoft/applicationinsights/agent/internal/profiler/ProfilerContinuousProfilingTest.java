// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class ProfilerContinuousProfilingTest {
  @TempDir File tempDir;

  private final TestTimeSource timeSource = new TestTimeSource();
  private ScheduledExecutorService executor;

  @AfterEach
  @SuppressWarnings("DirectInvocationOnMock")
  void tearDown() {
    if (executor != null && !mockingDetails(executor).isMock()) {
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

  private static AlertBreach targetedBreach(int profileDurationSeconds) {
    return manualBreach(profileDurationSeconds).toBuilder()
        .setSettingsMoniker("Portal_test")
        .setTargeted(true)
        .build();
  }

  @Test
  void targetedProfileUsesExactOneSecondOnDemandRecording() throws Exception {
    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.enableContinuousProfiling = true;
    config.continuousProfilingMaxAgeSeconds = 60;
    config.globalCooldownSeconds = 0;

    Recording continuousRecording = mock(Recording.class);
    Recording onDemandRecording = mock(Recording.class);
    AtomicInteger recordingCount = new AtomicInteger();

    Profiler profiler =
        new Profiler(config, tempDir, timeSource) {
          @Override
          protected Recording createRecording(RecordingOptions o, RecordingConfiguration c) {
            return recordingCount.getAndIncrement() == 0 ? continuousRecording : onDemandRecording;
          }
        };

    UploadService uploadService = mock(UploadService.class);
    FlightRecorderConnection frc = mock(FlightRecorderConnection.class);
    executor = mock(ScheduledExecutorService.class);

    Instant now = Instant.parse("2025-01-01T00:00:00Z");
    // The continuous recording has been running for longer than maxAge, so the circular buffer is
    // already full and the reported window covers the whole maxAge window.
    timeSource.setNow(now.minusSeconds(120));
    profiler.initialize(uploadService, executor, frc);

    // Continuous recording is started up-front and kept running.
    verify(continuousRecording).start();

    timeSource.setNow(now);
    UploadListener noOp = index -> {};

    profiler.profileAndUpload(targetedBreach(1), Duration.ofSeconds(1), noOp);

    verify(continuousRecording, never()).dump(anyString());
    verify(continuousRecording, never()).getStream(any(), any());
    verify(continuousRecording, never()).stop();
    verify(onDemandRecording).start();
    verify(executor).schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS));
    assertThat(profiler.isRecordingActive()).isTrue();

    Runnable rejectedDiagnostic = mock(Runnable.class);
    profiler.accept(manualBreach(1), noOp, rejectedDiagnostic);
    verify(continuousRecording, never()).dump(anyString());
    verify(rejectedDiagnostic, never()).run();
  }

  @Test
  void targetedProfileUsesExactMaximumDurationOnDemandRecording() throws Exception {
    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.enableContinuousProfiling = true;
    config.continuousProfilingMaxAgeSeconds = 60;
    config.globalCooldownSeconds = 0;

    Recording continuousRecording = mock(Recording.class);
    Recording onDemandRecording = mock(Recording.class);
    AtomicInteger recordingCount = new AtomicInteger();
    Profiler profiler =
        new Profiler(config, tempDir, timeSource) {
          @Override
          protected Recording createRecording(RecordingOptions o, RecordingConfiguration c) {
            return recordingCount.getAndIncrement() == 0 ? continuousRecording : onDemandRecording;
          }
        };

    UploadService uploadService = mock(UploadService.class);
    FlightRecorderConnection frc = mock(FlightRecorderConnection.class);
    executor = mock(ScheduledExecutorService.class);

    Instant now = Instant.parse("2025-01-01T00:00:00Z");
    // The continuous recording has been running for longer than maxAge, so the buffer is full.
    timeSource.setNow(now.minusSeconds(120));
    profiler.initialize(uploadService, executor, frc);

    verify(continuousRecording).start();

    timeSource.setNow(now);
    UploadListener noOp = index -> {};

    profiler.profileAndUpload(targetedBreach(360), Duration.ofMinutes(6), noOp);

    verify(continuousRecording, never()).clone(true);
    verify(continuousRecording, never()).dump(anyString());
    verify(continuousRecording, never()).getStream(any(), any());
    verify(continuousRecording, never()).stop();
    verify(onDemandRecording).start();
    verify(executor).schedule(any(Runnable.class), eq(360L), eq(TimeUnit.SECONDS));
    assertThat(profiler.isRecordingActive()).isTrue();
  }

  @Test
  void targetedProfileUploadFailureDoesNotEscape() throws Exception {
    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.enableContinuousProfiling = true;
    config.continuousProfilingMaxAgeSeconds = 60;
    config.globalCooldownSeconds = 0;

    Recording continuousRecording = mock(Recording.class);
    Recording onDemandRecording = mock(Recording.class);
    AtomicInteger recordingCount = new AtomicInteger();
    Profiler profiler =
        new Profiler(config, tempDir, timeSource) {
          @Override
          protected Recording createRecording(RecordingOptions o, RecordingConfiguration c) {
            return recordingCount.getAndIncrement() == 0 ? continuousRecording : onDemandRecording;
          }
        };

    UploadService uploadService = mock(UploadService.class);
    FlightRecorderConnection frc = mock(FlightRecorderConnection.class);
    executor = mock(ScheduledExecutorService.class);
    ArgumentCaptor<Runnable> scheduledUpload = ArgumentCaptor.forClass(Runnable.class);

    timeSource.setNow(Instant.parse("2025-01-01T00:00:00Z"));
    profiler.initialize(uploadService, executor, frc);
    profiler.profileAndUpload(targetedBreach(1), Duration.ofSeconds(1), index -> {});

    verify(executor).schedule(scheduledUpload.capture(), eq(1L), eq(TimeUnit.SECONDS));
    doThrow(new IllegalStateException("simulated upload failure"))
        .when(uploadService)
        .upload(any(), any(Long.class), any(File.class), any());

    assertThatCode(scheduledUpload.getValue()::run).doesNotThrowAnyException();
    assertThat(profiler.isRecordingActive()).isFalse();
  }

  @Test
  void targetedProfileCreationFailureDoesNotEscape() throws Exception {
    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.enableContinuousProfiling = true;
    config.continuousProfilingMaxAgeSeconds = 60;
    config.globalCooldownSeconds = 0;

    Recording continuousRecording = mock(Recording.class);
    AtomicInteger recordingCount = new AtomicInteger();
    Profiler profiler =
        new Profiler(config, tempDir, timeSource) {
          @Override
          protected Recording createRecording(RecordingOptions o, RecordingConfiguration c) {
            if (recordingCount.getAndIncrement() == 0) {
              return continuousRecording;
            }
            throw new IllegalStateException("simulated recording creation failure");
          }
        };

    UploadService uploadService = mock(UploadService.class);
    FlightRecorderConnection frc = mock(FlightRecorderConnection.class);
    executor = mock(ScheduledExecutorService.class);

    timeSource.setNow(Instant.parse("2025-01-01T00:00:00Z"));
    profiler.initialize(uploadService, executor, frc);

    assertThatCode(
            () -> profiler.profileAndUpload(targetedBreach(1), Duration.ofSeconds(1), index -> {}))
        .doesNotThrowAnyException();
    assertThat(profiler.isRecordingActive()).isFalse();
  }

  @Test
  void targetedProfileSchedulingFailureClosesRecordingAndDoesNotEscape() throws Exception {
    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.enableContinuousProfiling = true;
    config.continuousProfilingMaxAgeSeconds = 60;
    config.globalCooldownSeconds = 120;

    Recording continuousRecording = mock(Recording.class);
    Recording onDemandRecording = mock(Recording.class);
    AtomicInteger recordingCount = new AtomicInteger();
    Profiler profiler =
        new Profiler(config, tempDir, timeSource) {
          @Override
          protected Recording createRecording(RecordingOptions o, RecordingConfiguration c) {
            return recordingCount.getAndIncrement() == 0 ? continuousRecording : onDemandRecording;
          }
        };

    UploadService uploadService = mock(UploadService.class);
    FlightRecorderConnection frc = mock(FlightRecorderConnection.class);
    executor = mock(ScheduledExecutorService.class);
    doThrow(new IllegalStateException("simulated scheduling failure"))
        .when(executor)
        .schedule(any(Runnable.class), eq(1L), eq(TimeUnit.SECONDS));

    timeSource.setNow(Instant.parse("2025-01-01T00:00:00Z"));
    profiler.initialize(uploadService, executor, frc);

    assertThatCode(
            () -> profiler.profileAndUpload(targetedBreach(1), Duration.ofSeconds(1), index -> {}))
        .doesNotThrowAnyException();
    verify(onDemandRecording).close();
    assertThat(profiler.isRecordingActive()).isFalse();
    assertThat(profiler.getGlobalCooldownUntil()).isEqualTo(Instant.MIN);
  }

  @Test
  void continuousRecordingStartupFailureClosesRecordingAndDoesNotEscape() throws Exception {
    Configuration.ProfilerConfiguration config = new Configuration.ProfilerConfiguration();
    config.enableContinuousProfiling = true;
    config.continuousProfilingMaxAgeSeconds = 60;

    Recording continuousRecording = mock(Recording.class);
    doThrow(new IllegalStateException("simulated startup failure"))
        .when(continuousRecording)
        .start();
    Profiler profiler =
        new Profiler(config, tempDir, timeSource) {
          @Override
          protected Recording createRecording(RecordingOptions o, RecordingConfiguration c) {
            return continuousRecording;
          }
        };

    assertThatCode(
            () ->
                profiler.initialize(
                    mock(UploadService.class),
                    mock(ScheduledExecutorService.class),
                    mock(FlightRecorderConnection.class)))
        .doesNotThrowAnyException();
    verify(continuousRecording).close();
    assertThat(profiler.isContinuousRecordingRunning()).isFalse();
  }

  @Test
  void profileRequestSoonAfterStartupReportsActualCapturedWindow() throws Exception {
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

    Instant start = Instant.parse("2025-01-01T00:00:00Z");
    timeSource.setNow(start);
    profiler.initialize(uploadService, executor, frc);

    verify(continuousRecording).start();

    // Only 10s after startup the buffer holds far less than the 60s maxAge, so the reported window
    // must be clamped to the actual recording start rather than now - 60s.
    Instant now = start.plusSeconds(10);
    timeSource.setNow(now);
    UploadListener noOp = index -> {};

    profiler.profileAndUpload(manualBreach(10), Duration.ofSeconds(10), noOp);

    verify(continuousRecording).dump(anyString());
    verify(uploadService).upload(any(), eq(start.toEpochMilli()), any(File.class), any());
    assertThat(profiler.isRecordingActive()).isFalse();
  }
}
