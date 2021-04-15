package com.microsoft.applicationinsights.internal.profiler;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.profiler.config.AlertConfigParser;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.gcmonitor.GCCollectionEvent;
import com.microsoft.gcmonitor.GCEventConsumer;
import com.microsoft.gcmonitor.GcMonitorFactory;
import com.microsoft.gcmonitor.MemoryManagement;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.management.MBeanServerConnection;
import java.lang.management.MemoryUsage;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GcEventMonitorTest {

    @Test
    public void endToEndAlertIsTriggered() throws ExecutionException, InterruptedException, TimeoutException {

        CompletableFuture<AlertBreach> alertFuture = new CompletableFuture<>();
        AlertingSubsystem alertingSubsystem = getAlertingSubsystem(alertFuture);

        TelemetryClient client = new TelemetryClient() {
            @Override
            public void track(Telemetry telemetry) {
            }
        };

        GcMonitorFactory factory = new GcMonitorFactory() {
            @Override
            public MemoryManagement monitorSelf(ExecutorService executorService, GCEventConsumer consumer) {
                consumer.accept(mockGcEvent());
                return null;
            }

            @Override
            public MemoryManagement monitor(MBeanServerConnection connection, ExecutorService executorService, GCEventConsumer consumer) {
                return null;
            }
        };

        GcEventMonitor.init(
                alertingSubsystem,
                client,
                Executors.newSingleThreadExecutor(),
                new GcEventMonitor.GcEventMonitorConfiguration(GcReportingLevel.NONE),
                factory);

        AlertBreach alert = alertFuture.get(10, TimeUnit.SECONDS);

        Assert.assertEquals(90.0, alert.getAlertValue(), 0.01);
    }

    private AlertingSubsystem getAlertingSubsystem(CompletableFuture<AlertBreach> alertFuture) {
        AlertingSubsystem alertingSubsystem = AlertingSubsystem.create(alertFuture::complete, Executors.newSingleThreadExecutor());

        AlertingConfiguration config = AlertConfigParser.parse(
                "--cpu-trigger-enabled true --cpu-threshold 80 --cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400",
                "--memory-trigger-enabled true --memory-threshold 20 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400",
                "--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120",
                "--single --mode immediate --immediate-profiling-duration 120  --expiration 5249157885138288517 --settings-moniker a-settings-moniker"
        );

        alertingSubsystem.initialize(config);
        return alertingSubsystem;
    }

    private GCCollectionEvent mockGcEvent() {
        GCCollectionEvent event = Mockito.mock(GCCollectionEvent.class);
        GarbageCollector collector = Mockito.mock(GarbageCollector.class);
        MemoryPool tenuredPool = Mockito.mock(MemoryPool.class);
        Mockito.when(collector.isTenuredCollector()).thenReturn(true);
        Mockito.when(event.getCollector()).thenReturn(collector);
        Mockito.when(event.getTenuredPool()).thenReturn(Optional.of(tenuredPool));
        Mockito.when(event.getMemoryUsageAfterGc(Mockito.eq(tenuredPool))).thenReturn(new MemoryUsage(1, 9, 10, 10));
        return event;
    }
}
