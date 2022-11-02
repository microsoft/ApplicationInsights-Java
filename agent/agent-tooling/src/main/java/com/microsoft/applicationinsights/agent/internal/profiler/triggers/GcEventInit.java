// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.EventTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedTime;
import com.microsoft.applicationinsights.agent.internal.configuration.GcReportingLevel;
import com.microsoft.applicationinsights.agent.internal.profiler.util.ServiceLoaderUtil;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.gcmonitor.GcCollectionEvent;
import com.microsoft.gcmonitor.GcEventConsumer;
import com.microsoft.gcmonitor.GcMonitorFactory;
import com.microsoft.gcmonitor.UnableToMonitorMemoryException;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.lang.management.MemoryUsage;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors GC events. Forwards relevant metrics to the alerting subsystem.
 *
 * <p>If reportAllGcEvents configuration setting is set, reports GC event to Application Insights
 */
class GcEventInit {

  private static final Logger logger = LoggerFactory.getLogger(GcEventInit.class);

  // a unique jvm_instance_id is needed for every restart as the gc starts again from scratch every
  // time the JVM is restarted, and we need to analyze single JVM execution
  // TODO if/when Application Insights adds a unique ID that represents a single JVM, pull that ID
  // here
  private static final String JVM_INSTANCE_UID = UUID.randomUUID().toString();

  static class GcEventMonitorConfiguration {

    final GcReportingLevel reportingLevel;

    GcEventMonitorConfiguration(GcReportingLevel reportingLevel) {
      this.reportingLevel = reportingLevel;
    }
  }

  /** Initialise GC monitoring. */
  static void init(
      AlertingSubsystem alertingSubsystem,
      TelemetryClient telemetryClient,
      ExecutorService executorService,
      GcEventMonitorConfiguration gcEventMonitorConfiguration) {

    GcMonitorFactory gcMonitorFactory = ServiceLoaderUtil.findServiceLoader(GcMonitorFactory.class);

    if (gcMonitorFactory != null) {
      init(
          alertingSubsystem,
          telemetryClient,
          executorService,
          gcEventMonitorConfiguration,
          gcMonitorFactory);
    }
  }

  static void init(
      AlertingSubsystem alertingSubsystem,
      TelemetryClient telemetryClient,
      ExecutorService executorService,
      GcEventMonitorConfiguration gcEventMonitorConfiguration,
      GcMonitorFactory gcMonitorFactory) {
    try {
      gcMonitorFactory.monitorSelf(
          executorService,
          process(alertingSubsystem, telemetryClient, gcEventMonitorConfiguration));
    } catch (UnableToMonitorMemoryException e) {
      logger.error("Failed to monitor gc mxbeans");
    }
  }

  /** Consumer of a GC event. */
  private static GcEventConsumer process(
      AlertingSubsystem alertingSubsystem,
      TelemetryClient telemetryClient,
      GcEventMonitorConfiguration gcEventMonitorConfiguration) {
    return event -> {
      sendTenuredFillPercentageToAlerting(alertingSubsystem, event);
      emitGcEvent(telemetryClient, gcEventMonitorConfiguration, event);
    };
  }

  /** Calculate the tenured fill percentage and forward the data to the alerting subsystem. */
  private static void sendTenuredFillPercentageToAlerting(
      AlertingSubsystem alertingSubsystem, GcCollectionEvent event) {
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

  /** If gc reporting is enabled, send gc event to Application Insights. */
  private static void emitGcEvent(
      TelemetryClient telemetryClient,
      GcEventMonitorConfiguration gcEventMonitorConfiguration,
      GcCollectionEvent event) {
    boolean reportEvent = gcEventMonitorConfiguration.reportingLevel == GcReportingLevel.ALL;
    reportEvent |=
        gcEventMonitorConfiguration.reportingLevel == GcReportingLevel.TENURED_ONLY
            && event.getCollector().isTenuredCollector();

    if (!reportEvent) {
      return;
    }

    EventTelemetryBuilder telemetryBuilder = telemetryClient.newEventTelemetryBuilder();

    telemetryBuilder.setName("GcEvent");

    telemetryBuilder.addProperty("collector", event.getCollector().getName());
    telemetryBuilder.addProperty("type", event.getGcCause());
    telemetryBuilder.addProperty("action", event.getGcAction());
    telemetryBuilder.addProperty("jvm_instance_id", JVM_INSTANCE_UID);

    telemetryBuilder.addMeasurement("id", (double) event.getId());
    telemetryBuilder.addMeasurement("duration_ms", (double) event.getDuration());
    telemetryBuilder.addMeasurement("end_time_ms", (double) event.getEndTime());
    telemetryBuilder.addMeasurement("thread_count", (double) event.getGcThreadCount());
    telemetryBuilder.addMeasurement(
        "collection_count", (double) event.getCollector().getCollectionCount());
    telemetryBuilder.addMeasurement(
        "cumulative_collector_time_sec", (double) event.getCollector().getCollectionTime());

    addMemoryUsage(
        "young", "before", telemetryBuilder, event.getMemoryUsageBeforeGc(event.getYoungPools()));
    addMemoryUsage(
        "young", "after", telemetryBuilder, event.getMemoryUsageAfterGc(event.getYoungPools()));

    Optional<MemoryPool> tenuredPool = event.getTenuredPool();
    if (tenuredPool.isPresent()) {
      MemoryUsage beforeOg = event.getMemoryUsageBeforeGc(tenuredPool.get());
      addMemoryUsage("tenured", "before", telemetryBuilder, beforeOg);

      MemoryUsage afterOg = event.getMemoryUsageAfterGc(tenuredPool.get());
      addMemoryUsage("tenured", "after", telemetryBuilder, afterOg);
    }

    telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());

    telemetryClient.trackAsync(telemetryBuilder.build());
  }

  private static void addMemoryUsage(
      String poolName, String when, EventTelemetryBuilder telemetryBuilder, MemoryUsage memory) {
    telemetryBuilder.addMeasurement(poolName + "_" + when + "_used", (double) memory.getUsed());
    telemetryBuilder.addMeasurement(
        poolName + "_" + when + "_size", (double) memory.getCommitted());
    telemetryBuilder.addMeasurement(poolName + "_max", (double) memory.getMax());
  }

  private GcEventInit() {}
}
