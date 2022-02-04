/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.microsoft.applicationinsights.agent.internal.configuration.GcReportingLevel;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.EventTelemetry;
import com.microsoft.applicationinsights.agent.internal.telemetry.FormattedTime;
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
public class GcEventMonitor {
  private static final Logger LOGGER = LoggerFactory.getLogger(GcEventMonitor.class);

  // a unique jvm_instance_id is needed for every restart as the gc starts again from scratch every
  // time
  // the JVM is restarted, and we need to analyze single JVM execution
  // TODO if/when Application Insights adds a unique ID that represents a single JVM, pull that ID
  // here
  private static final String JVM_INSTANCE_UID = UUID.randomUUID().toString();

  public static class GcEventMonitorConfiguration {
    public final GcReportingLevel reportingLevel;

    public GcEventMonitorConfiguration(GcReportingLevel reportingLevel) {
      this.reportingLevel = reportingLevel;
    }
  }

  /** Initialise GC monitoring. */
  public static void init(
      AlertingSubsystem alertingSubsystem,
      TelemetryClient telemetryClient,
      ExecutorService executorService,
      GcEventMonitorConfiguration gcEventMonitorConfiguration) {
    GcMonitorFactory gcMonitorFactory =
        ProfilerServiceInitializer.findServiceLoader(GcMonitorFactory.class);

    if (gcMonitorFactory != null) {
      init(
          alertingSubsystem,
          telemetryClient,
          executorService,
          gcEventMonitorConfiguration,
          gcMonitorFactory);
    }
  }

  public static void init(
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
      LOGGER.error("Failed to monitor gc mxbeans");
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

    EventTelemetry telemetry = telemetryClient.newEventTelemetry();

    telemetry.setName("GcEvent");

    telemetry.addProperty("collector", event.getCollector().getName());
    telemetry.addProperty("type", event.getGcCause());
    telemetry.addProperty("action", event.getGcAction());
    telemetry.addProperty("jvm_instance_id", JVM_INSTANCE_UID);

    telemetry.addMeasurement("id", (double) event.getId());
    telemetry.addMeasurement("duration_ms", (double) event.getDuration());
    telemetry.addMeasurement("end_time_ms", (double) event.getEndTime());
    telemetry.addMeasurement("thread_count", (double) event.getGcThreadCount());
    telemetry.addMeasurement(
        "collection_count", (double) event.getCollector().getCollectionCount());
    telemetry.addMeasurement(
        "cumulative_collector_time_sec", (double) event.getCollector().getCollectionTime());

    addMemoryUsage(
        "young", "before", telemetry, event.getMemoryUsageBeforeGc(event.getYoungPools()));
    addMemoryUsage("young", "after", telemetry, event.getMemoryUsageAfterGc(event.getYoungPools()));

    Optional<MemoryPool> tenuredPool = event.getTenuredPool();
    if (tenuredPool.isPresent()) {
      MemoryUsage beforeOg = event.getMemoryUsageBeforeGc(tenuredPool.get());
      addMemoryUsage("tenured", "before", telemetry, beforeOg);

      MemoryUsage afterOg = event.getMemoryUsageAfterGc(tenuredPool.get());
      addMemoryUsage("tenured", "after", telemetry, afterOg);
    }

    telemetry.setTime(FormattedTime.offSetDateTimeFromNow());

    telemetryClient.trackAsync(telemetry);
  }

  private static void addMemoryUsage(
      String poolName, String when, EventTelemetry telemetry, MemoryUsage memory) {
    telemetry.addMeasurement(poolName + "_" + when + "_used", (double) memory.getUsed());
    telemetry.addMeasurement(poolName + "_" + when + "_size", (double) memory.getCommitted());
    telemetry.addMeasurement(poolName + "_max", (double) memory.getMax());
  }

  private GcEventMonitor() {}
}
