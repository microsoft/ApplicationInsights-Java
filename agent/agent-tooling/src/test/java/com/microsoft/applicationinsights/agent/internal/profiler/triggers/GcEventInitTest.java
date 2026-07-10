// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryEventData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.statsbeat.StatsbeatModule;
import com.microsoft.applicationinsights.agent.internal.configuration.GcReportingLevel;
import com.microsoft.applicationinsights.agent.internal.profiler.testutil.TestTimeSource;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplerUtil;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingProfileFileTriggerConfiguration;
import com.microsoft.gcmonitor.GcCollectionEvent;
import com.microsoft.gcmonitor.GcEventConsumer;
import com.microsoft.gcmonitor.GcMonitorFactory;
import com.microsoft.gcmonitor.MemoryManagement;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import io.opentelemetry.sdk.resources.Resource;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import javax.management.MBeanServerConnection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GcEventInitTest {

  private static final String FAKE_CONNECTION_STRING =
      "InstrumentationKey=00000000-0000-0000-0000-000000000000;IngestionEndpoint=https://fake-ingestion-endpoint.example.com/";

  @Test
  void gcEventIsEmittedWithSampleRateThatBypassesIngestionSampling()
      throws ExecutionException, InterruptedException, TimeoutException {

    CompletableFuture<TelemetryItem> gcEventFuture = new CompletableFuture<>();
    Consumer<TelemetryItem> observer =
        telemetryItem -> {
          if (telemetryItem.getData().getBaseData() instanceof TelemetryEventData
              && "GcEvent"
                  .equals(((TelemetryEventData) telemetryItem.getData().getBaseData()).getName())) {
            gcEventFuture.complete(telemetryItem);
          }
        };
    TelemetryObservers.INSTANCE.getObservers().add(observer);

    try {
      CompletableFuture<AlertBreach> alertFuture = new CompletableFuture<>();
      TestTimeSource timeSource = new TestTimeSource();
      AlertingSubsystem alertingSubsystem = getAlertingSubsystem(alertFuture, timeSource);

      GcMonitorFactory factory =
          new GcMonitorFactory() {
            @Override
            public MemoryManagement monitorSelf(
                ExecutorService executorService, GcEventConsumer consumer) {
              consumer.accept(fullyMockedGcEvent());
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

      TelemetryClient telemetryClient =
          TelemetryClient.builder()
              .setCustomDimensions(new HashMap<>())
              .setMetricFilters(new ArrayList<>())
              .setStatsbeatModule(new StatsbeatModule(response -> {}))
              .setConnectionStrings(FAKE_CONNECTION_STRING)
              .build();
      telemetryClient.setOtelResource(Resource.empty());

      GcEventInit.init(
          alertingSubsystem,
          telemetryClient,
          Executors.newSingleThreadExecutor(),
          new GcEventInit.GcEventMonitorConfiguration(GcReportingLevel.ALL),
          factory);

      TelemetryItem gcEvent = gcEventFuture.get(10, TimeUnit.SECONDS);

      assertThat(gcEvent.getSampleRate())
          .isEqualTo((float) SamplerUtil.SAMPLE_RATE_TO_DISABLE_INGESTION_SAMPLING);
    } finally {
      TelemetryObservers.INSTANCE.getObservers().remove(observer);
    }
  }

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
        AlertingSubsystem.create(
            alertFuture::complete,
            timeSource,
            AlertingProfileFileTriggerConfiguration.createDefault());

    AlertingConfiguration config =
        AlertConfigParser.parse(
            "--cpu-trigger-enabled true --cpu-threshold 80 --cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400",
            "--memory-trigger-enabled true --memory-threshold 20 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400",
            "--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120",
            "--single --mode immediate --immediate-profiling-duration 120  --expiration 5249157885138288517 --settings-moniker a-settings-moniker",
            null);

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

  private static GcCollectionEvent fullyMockedGcEvent() {
    GcCollectionEvent event = Mockito.mock(GcCollectionEvent.class);
    GarbageCollector collector = Mockito.mock(GarbageCollector.class);
    MemoryPool tenuredPool = Mockito.mock(MemoryPool.class);
    MemoryUsage memoryUsage = new MemoryUsage(1, 9, 10, 10);

    Mockito.when(collector.isTenuredCollector()).thenReturn(true);
    Mockito.when(collector.getName()).thenReturn("test-collector");
    Mockito.when(event.getCollector()).thenReturn(collector);
    Mockito.when(event.getGcCause()).thenReturn("test-cause");
    Mockito.when(event.getGcAction()).thenReturn("test-action");
    Mockito.when(event.getTenuredPool()).thenReturn(Optional.of(tenuredPool));
    Mockito.when(event.getMemoryUsageBeforeGc(Mockito.any(MemoryPool.class)))
        .thenReturn(memoryUsage);
    Mockito.when(event.getMemoryUsageAfterGc(Mockito.any(MemoryPool.class)))
        .thenReturn(memoryUsage);
    Mockito.when(event.getMemoryUsageBeforeGc(Mockito.anyList())).thenReturn(memoryUsage);
    Mockito.when(event.getMemoryUsageAfterGc(Mockito.anyList())).thenReturn(memoryUsage);
    return event;
  }
}
