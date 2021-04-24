package com.microsoft.applicationinsights.internal.profiler;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryEventData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryUtil;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertMetricType;
import com.microsoft.gcmonitor.GCCollectionEvent;
import com.microsoft.gcmonitor.GCEventConsumer;
import com.microsoft.gcmonitor.GcMonitorFactory;
import com.microsoft.gcmonitor.UnableToMonitorMemoryException;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.MemoryUsage;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Monitors GC events.  Forwards relevant metrics to the alerting subsystem.
 * <p>
 * If reportAllGcEvents configuration setting is set, reports GC event to Application Insights
 */
public class GcEventMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcEventMonitor.class);

    public static class GcEventMonitorConfiguration {
        public final GcReportingLevel reportingLevel;

        public GcEventMonitorConfiguration(GcReportingLevel reportingLevel) {
            this.reportingLevel = reportingLevel;
        }
    }

    /**
     * Initialise GC monitoring
     */
    public static void init(
            AlertingSubsystem alertingSubsystem,
            TelemetryClient telemetryClient,
            ExecutorService executorService,
            GcEventMonitorConfiguration gcEventMonitorConfiguration) {
        GcMonitorFactory gcMonitorFactory = ProfilerServiceInitializer.findServiceLoader(GcMonitorFactory.class);

        if (gcMonitorFactory != null) {
            init(alertingSubsystem, telemetryClient, executorService, gcEventMonitorConfiguration, gcMonitorFactory);
        }
    }

    public static void init(
            AlertingSubsystem alertingSubsystem,
            TelemetryClient telemetryClient,
            ExecutorService executorService,
            GcEventMonitorConfiguration gcEventMonitorConfiguration,
            GcMonitorFactory gcMonitorFactory) {
        try {
            gcMonitorFactory.monitorSelf(executorService, process(alertingSubsystem, telemetryClient, gcEventMonitorConfiguration));
        } catch (UnableToMonitorMemoryException e) {
            LOGGER.error("Failed to monitor gc mxbeans");
        }
    }

    /**
     * Consumer of a GC event
     */
    private static GCEventConsumer process(AlertingSubsystem alertingSubsystem,
                                           TelemetryClient telemetryClient,
                                           GcEventMonitorConfiguration gcEventMonitorConfiguration) {
        return event -> {
            sendTenuredFillPercentageToAlerting(alertingSubsystem, event);
            emitGcEvent(telemetryClient, gcEventMonitorConfiguration, event);
        };
    }

    /**
     * Calculate the tenured fill percentage and forward the data to the alerting subsystem
     */
    private static void sendTenuredFillPercentageToAlerting(AlertingSubsystem alertingSubsystem, GCCollectionEvent event) {
        if (event.getCollector().isTenuredCollector()) {
            Optional<MemoryPool> tenuredPool = event.getTenuredPool();
            if (tenuredPool.isPresent()) {
                MemoryUsage tenuredUsage = event.getMemoryUsageAfterGc(tenuredPool.get());
                long currentLevel = tenuredUsage.getUsed();
                long max = tenuredUsage.getMax();
                if (max > 0) {
                    double percentage = 100.0 * (double) currentLevel / (double) max;
                    alertingSubsystem.track(AlertMetricType.MEMORY, percentage);
                }
            }
        }
    }

    /**
     * If gc reporting is enabled, send gc event to Application Insights
     */
    private static void emitGcEvent(TelemetryClient telemetryClient, GcEventMonitorConfiguration gcEventMonitorConfiguration, GCCollectionEvent event) {
        boolean reportEvent = gcEventMonitorConfiguration.reportingLevel == GcReportingLevel.ALL;
        reportEvent |= gcEventMonitorConfiguration.reportingLevel == GcReportingLevel.TENURED_ONLY && event.getCollector().isTenuredCollector();

        if (!reportEvent) {
            return;
        }

        TelemetryEventData data = new TelemetryEventData();
        data.setName("GcEvent");

        data.getProperties().put("collector", event.getCollector().getName());
        data.getProperties().put("type", event.getGcCause());
        data.getProperties().put("action", event.getGcAction());
        data.getMeasurements().put("id", (double) event.getId());
        data.getMeasurements().put("duration_ms", (double) event.getDuration());
        data.getMeasurements().put("end_time_ms", (double) event.getEndTime());
        data.getMeasurements().put("thread_count", (double) event.getGcThreadCount());
        data.getMeasurements().put("collection_count", (double) event.getCollector().getCollectionCount());
        data.getMeasurements().put("cumulative_collector_time_sec", (double) event.getCollector().getCollectionTime());

        addMemoryUsage("young", "before", data, event.getMemoryUsageBeforeGc(event.getYoungPools()));
        addMemoryUsage("young", "after", data, event.getMemoryUsageAfterGc(event.getYoungPools()));

        Optional<MemoryPool> tenuredPool = event.getTenuredPool();
        if (tenuredPool.isPresent()) {
            MemoryUsage beforeOg = event.getMemoryUsageBeforeGc(tenuredPool.get());
            addMemoryUsage("tenured", "before", data, beforeOg);

            MemoryUsage afterOg = event.getMemoryUsageAfterGc(tenuredPool.get());
            addMemoryUsage("tenured", "after", data, afterOg);
        }

        TelemetryItem telemetry = TelemetryUtil.createTelemetry(data);
        telemetry.setTime(TelemetryUtil.currentTime());
        telemetryClient.track(telemetry);
    }

    private static void addMemoryUsage(String poolName, String when, TelemetryEventData data, MemoryUsage memory) {
        data.getMeasurements().put(poolName + "_" + when + "_used", (double) memory.getUsed());
        data.getMeasurements().put(poolName + "_" + when + "_size", (double) memory.getCommitted());
        data.getMeasurements().put(poolName + "_max", (double) memory.getMax());
    }
}
