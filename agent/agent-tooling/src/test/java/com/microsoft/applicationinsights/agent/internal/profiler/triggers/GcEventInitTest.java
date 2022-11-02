// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.agent.internal.configuration.GcReportingLevel;
import com.microsoft.applicationinsights.agent.internal.profiler.testutil.TestTimeSource;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.gcmonitor.GcCollectionEvent;
import com.microsoft.gcmonitor.GcEventConsumer;
import com.microsoft.gcmonitor.GcMonitorFactory;
import com.microsoft.gcmonitor.MemoryManagement;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.lang.management.MemoryUsage;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.management.MBeanServerConnection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GcEventInitTest {

  @Test
  void endToEndAlertIsTriggered()
      throws ExecutionException, InterruptedException, TimeoutException {

    CompletableFuture<AlertBreach> alertFuture = new CompletableFuture<>();
    TestTimeSource timeSource = new TestTimeSource();
    AlertingSubsystem alertingSubsystem = getAlertingSubsystem(alertFuture, timeSource);

    GcMonitorFactory factory =
        new GcMonitorFactory() {
          @Override
          public MemoryManagement monitorSelf(
              ExecutorService executorService, GcEventConsumer consumer) {
            consumer.accept(mockGcEvent());
            return null;
          }

          @Override
          public MemoryManagement monitor(
              MBeanServerConnection connection,
              ExecutorService executorService,
              GcEventConsumer consumer) {
            return null;
          }
        };

    GcEventInit.init(
        alertingSubsystem,
        TelemetryClient.createForTest(),
        Executors.newSingleThreadExecutor(),
        new GcEventInit.GcEventMonitorConfiguration(GcReportingLevel.NONE),
        factory);

    AlertBreach alert = alertFuture.get(10, TimeUnit.SECONDS);

    assertThat(alert.getAlertValue()).isEqualTo(90.0);
  }

  private static AlertingSubsystem getAlertingSubsystem(
      CompletableFuture<AlertBreach> alertFuture, TimeSource timeSource) {
    AlertingSubsystem alertingSubsystem =
        AlertingSubsystem.create(alertFuture::complete, timeSource);

    AlertingConfiguration config =
        AlertConfigParser.parse(
            "--cpu-trigger-enabled true --cpu-threshold 80 --cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400",
            "--memory-trigger-enabled true --memory-threshold 20 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400",
            "--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120",
            "--single --mode immediate --immediate-profiling-duration 120  --expiration 5249157885138288517 --settings-moniker a-settings-moniker");

    alertingSubsystem.initialize(config);
    return alertingSubsystem;
  }

  private static GcCollectionEvent mockGcEvent() {
    GcCollectionEvent event = Mockito.mock(GcCollectionEvent.class);
    GarbageCollector collector = Mockito.mock(GarbageCollector.class);
    MemoryPool tenuredPool = Mockito.mock(MemoryPool.class);
    Mockito.when(collector.isTenuredCollector()).thenReturn(true);
    Mockito.when(event.getCollector()).thenReturn(collector);
    Mockito.when(event.getTenuredPool()).thenReturn(Optional.of(tenuredPool));
    Mockito.when(event.getMemoryUsageAfterGc(Mockito.eq(tenuredPool)))
        .thenReturn(new MemoryUsage(1, 9, 10, 10));
    return event;
  }
}
