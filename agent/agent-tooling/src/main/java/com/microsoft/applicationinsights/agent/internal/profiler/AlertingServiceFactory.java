// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import static com.microsoft.applicationinsights.agent.internal.perfcounter.MetricNames.TOTAL_CPU_PERCENTAGE;

import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorDomain;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.GcReportingLevel;
import com.microsoft.applicationinsights.agent.internal.profiler.triggers.RequestAlertPipelineBuilder;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipeline;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipelineMultiplexer;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Creates AlertMonitor and wires it up to observe telemetry. */
public class AlertingServiceFactory {

  // TODO (trask) inject instead of using global
  private static volatile AlertingSubsystem alertingSubsystem;

  static AlertingSubsystem create(
      Configuration configuration,
      Consumer<AlertBreach> alertAction,
      TelemetryObservers telemetryObservers,
      TelemetryClient telemetryClient,
      ExecutorService executorService) {
    alertingSubsystem = AlertingSubsystem.create(alertAction, TimeSource.DEFAULT);

    if (configuration.preview.profiler.enableRequestTriggering) {
      List<AlertPipeline> spanPipelines =
          Arrays.stream(configuration.preview.profiler.requestTriggerEndpoints)
              .map(it -> RequestAlertPipelineBuilder.build(it, alertAction, TimeSource.DEFAULT))
              .collect(Collectors.toList());

      alertingSubsystem.setPipeline(
          AlertMetricType.REQUEST, new AlertPipelineMultiplexer(spanPipelines));
    }

    addObserver(alertingSubsystem, telemetryObservers);

    monitorGcActivity(
        alertingSubsystem,
        telemetryClient,
        executorService,
        formGcEventMonitorConfiguration(configuration.preview));
    return alertingSubsystem;
  }

  private static GcEventMonitor.GcEventMonitorConfiguration formGcEventMonitorConfiguration(
      Configuration.PreviewConfiguration configuration) {
    if (configuration.gcEvents.reportingLevel != null) {
      return new GcEventMonitor.GcEventMonitorConfiguration(configuration.gcEvents.reportingLevel);
    }

    // The memory monitoring requires observing gc events
    if (configuration.profiler.enabled) {
      return new GcEventMonitor.GcEventMonitorConfiguration(GcReportingLevel.TENURED_ONLY);
    }

    return new GcEventMonitor.GcEventMonitorConfiguration(GcReportingLevel.NONE);
  }

  private static void monitorGcActivity(
      AlertingSubsystem alertingSubsystem,
      TelemetryClient telemetryClient,
      ExecutorService executorService,
      GcEventMonitor.GcEventMonitorConfiguration gcEventMonitorConfiguration) {
    GcEventMonitor.init(
        alertingSubsystem, telemetryClient, executorService, gcEventMonitorConfiguration);
  }

  private static void addObserver(
      AlertingSubsystem alertingSubsystem, TelemetryObservers telemetryObservers) {
    telemetryObservers.addObserver(
        telemetry -> {
          MonitorDomain data = telemetry.getData().getBaseData();
          if (!(data instanceof MetricsData)) {
            return;
          }
          MetricDataPoint point = ((MetricsData) data).getMetrics().get(0);
          AlertMetricType alertMetricType = null;
          if (point.getName().equals(TOTAL_CPU_PERCENTAGE)) {
            alertMetricType = AlertMetricType.CPU;
          }

          if (alertMetricType != null) {
            alertingSubsystem.track(alertMetricType, point.getValue());
          }
        });
  }

  public static AlertingSubsystem getAlertingSubsystem() {
    return alertingSubsystem;
  }

  private AlertingServiceFactory() {}
}
