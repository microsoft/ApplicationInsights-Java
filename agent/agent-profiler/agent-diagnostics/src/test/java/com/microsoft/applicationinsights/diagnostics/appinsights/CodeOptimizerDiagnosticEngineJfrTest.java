// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.appinsights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CodeOptimizerDiagnosticEngineJfrTest {

  /**
   * Engine subclass that avoids the real JFR subsystem and simply records how many times the
   * diagnostic cycle is started/ended so tests can assert on lifecycle behavior.
   */
  private static class RecordingEngine extends CodeOptimizerDiagnosticEngineJfr {
    final AtomicInteger startCycleCount = new AtomicInteger();
    final AtomicInteger endCycleCount = new AtomicInteger();
    final AtomicInteger emitInfoCount = new AtomicInteger();

    RecordingEngine(ScheduledExecutorService executorService) {
      super(executorService, Paths.get("/"));
    }

    @Override
    protected boolean isOsSupported() {
      return true;
    }

    @Override
    protected void startDiagnosticCycle() {
      startCycleCount.incrementAndGet();
    }

    @Override
    protected void endDiagnosticCycle() {
      endCycleCount.incrementAndGet();
    }

    @Override
    protected void emitInfo(AlertBreach alert) {
      emitInfoCount.incrementAndGet();
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

  private static Runnable captureScheduledShutdown(
      ScheduledExecutorService executor, long expectedDelaySeconds) {
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
    verify(executor, times(2))
        .schedule(runnableCaptor.capture(), delayCaptor.capture(), eq(TimeUnit.SECONDS));
    List<Runnable> runnables = runnableCaptor.getAllValues();
    List<Long> delays = delayCaptor.getAllValues();
    for (int i = 0; i < delays.size(); i++) {
      if (delays.get(i) == expectedDelaySeconds) {
        return runnables.get(i);
      }
    }
    throw new AssertionError("No task scheduled with delay " + expectedDelaySeconds);
  }

  @Test
  void breachDuringStartupDoesNotTearDownContinuousEmitters() {
    ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    when(executor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(null);

    RecordingEngine engine = new RecordingEngine(executor);

    // A breach is processed during startup, before continuous diagnostics has been enabled.
    int profileDurationSeconds = 60;
    long end =
        profileDurationSeconds
            - CodeOptimizerDiagnosticEngineJfr.TIME_BEFORE_END_OF_PROFILE_TO_EMIT_EVENT;
    engine.performDiagnosis(manualBreach(profileDurationSeconds));

    Runnable shutdown = captureScheduledShutdown(executor, end);

    // Continuous diagnostics is enabled after the breach was scheduled but before the stale
    // shutdown fires.
    engine.startContinuousDiagnostics();

    // The stale shutdown from the initial breach now fires.
    shutdown.run();

    // It must NOT tear down the continuously-registered emitters.
    assertThat(engine.endCycleCount.get()).isZero();
  }

  @Test
  void breachWithoutContinuousDiagnosticsTearsDownCycle() {
    ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    when(executor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(null);

    RecordingEngine engine = new RecordingEngine(executor);

    int profileDurationSeconds = 60;
    long end =
        profileDurationSeconds
            - CodeOptimizerDiagnosticEngineJfr.TIME_BEFORE_END_OF_PROFILE_TO_EMIT_EVENT;
    engine.performDiagnosis(manualBreach(profileDurationSeconds));

    Runnable shutdown = captureScheduledShutdown(executor, end);
    shutdown.run();

    // With no continuous diagnostics, the cycle is torn down as before.
    assertThat(engine.endCycleCount.get()).isEqualTo(1);
  }
}
