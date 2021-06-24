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

package com.microsoft.applicationinsights.internal.profiler;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryEventData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.FormattedTime;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertMetricType;
import com.microsoft.gcmonitor.GCCollectionEvent;
import com.microsoft.gcmonitor.GCEventConsumer;
import com.microsoft.gcmonitor.GcMonitorFactory;
import com.microsoft.gcmonitor.UnableToMonitorMemoryException;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
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

  /** Initialise GC monitoring */
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

  /** Consumer of a GC event */
  private static GCEventConsumer process(
      AlertingSubsystem alertingSubsystem,
      TelemetryClient telemetryClient,
      GcEventMonitorConfiguration gcEventMonitorConfiguration) {
    return event -> {
      sendTenuredFillPercentageToAlerting(alertingSubsystem, event);
      emitGcEvent(telemetryClient, gcEventMonitorConfiguration, event);
    };
  }

  /** Calculate the tenured fill percentage and forward the data to the alerting subsystem */
  private static void sendTenuredFillPercentageToAlerting(
      AlertingSubsystem alertingSubsystem, GCCollectionEvent event) {
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

  /** If gc reporting is enabled, send gc event to Application Insights */
  private static void emitGcEvent(
      TelemetryClient telemetryClient,
      GcEventMonitorConfiguration gcEventMonitorConfiguration,
      GCCollectionEvent event) {
    boolean reportEvent = gcEventMonitorConfiguration.reportingLevel == GcReportingLevel.ALL;
    reportEvent |=
        gcEventMonitorConfiguration.reportingLevel == GcReportingLevel.TENURED_ONLY
            && event.getCollector().isTenuredCollector();

    if (!reportEvent) {
      return;
    }

    TelemetryItem telemetry = new TelemetryItem();
    TelemetryEventData data = new TelemetryEventData();
    TelemetryClient.getActive().initEventTelemetry(telemetry, data);

    data.setName("GcEvent");

    Map<String, String> properties = new HashMap<>();
    properties.put("collector", event.getCollector().getName());
    properties.put("type", event.getGcCause());
    properties.put("action", event.getGcAction());
    properties.put("jvm_instance_id", JVM_INSTANCE_UID);
    data.setProperties(properties);

    Map<String, Double> measurements = new HashMap<>();
    measurements.put("id", (double) event.getId());
    measurements.put("duration_ms", (double) event.getDuration());
    measurements.put("end_time_ms", (double) event.getEndTime());
    measurements.put("thread_count", (double) event.getGcThreadCount());
    measurements.put("collection_count", (double) event.getCollector().getCollectionCount());
    measurements.put(
        "cumulative_collector_time_sec", (double) event.getCollector().getCollectionTime());

    addMemoryUsage(
        "young", "before", measurements, event.getMemoryUsageBeforeGc(event.getYoungPools()));
    addMemoryUsage(
        "young", "after", measurements, event.getMemoryUsageAfterGc(event.getYoungPools()));

    Optional<MemoryPool> tenuredPool = event.getTenuredPool();
    if (tenuredPool.isPresent()) {
      MemoryUsage beforeOg = event.getMemoryUsageBeforeGc(tenuredPool.get());
      addMemoryUsage("tenured", "before", measurements, beforeOg);

      MemoryUsage afterOg = event.getMemoryUsageAfterGc(tenuredPool.get());
      addMemoryUsage("tenured", "after", measurements, afterOg);
    }
    data.setMeasurements(measurements);

    telemetry.setTime(FormattedTime.fromNow());

    telemetryClient.trackAsync(telemetry);
  }

  private static void addMemoryUsage(
      String poolName, String when, Map<String, Double> measurements, MemoryUsage memory) {
    measurements.put(poolName + "_" + when + "_used", (double) memory.getUsed());
    measurements.put(poolName + "_" + when + "_size", (double) memory.getCommitted());
    measurements.put(poolName + "_max", (double) memory.getMax());
  }

  private GcEventMonitor() {}
}
