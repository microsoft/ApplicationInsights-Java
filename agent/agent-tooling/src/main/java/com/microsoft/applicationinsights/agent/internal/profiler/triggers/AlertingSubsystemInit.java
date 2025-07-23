// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import static com.microsoft.applicationinsights.agent.internal.perfcounter.MetricNames.TOTAL_CPU_PERCENTAGE;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.builders.EventTelemetryBuilder;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.builders.MessageTelemetryBuilder;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.models.MonitorDomain;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.FormattedTime;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.GcReportingLevel;
import com.microsoft.applicationinsights.agent.internal.profiler.Profiler;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.ServiceProfilerIndex;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryObservers;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipeline;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipelineMultiplexer;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.diagnostics.DiagnosticEngine;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates AlertMonitor and wires it up to observe telemetry. */
public class AlertingSubsystemInit {

  // TODO (trask) inject instead of using global
  private static volatile AlertingSubsystem alertingSubsystem;

  private static final Logger logger = LoggerFactory.getLogger(AlertingSubsystemInit.class);

  public static AlertingSubsystem create(
      Configuration.ProfilerConfiguration configuration,
      GcReportingLevel reportingLevel,
      TelemetryObservers telemetryObservers,
      Profiler profiler,
      TelemetryClient telemetryClient,
      DiagnosticEngine diagnosticEngine,
      ExecutorService executorService) {

    // TODO (trask) delay creation of AlertingSubsystem until after Profiler is created and
    // initialized?
    Consumer<AlertBreach> alertAction =
        alert -> alertAction(alert, profiler, diagnosticEngine, telemetryClient);

    alertingSubsystem = AlertingSubsystem.create(alertAction, TimeSource.DEFAULT);

    if (configuration.enableRequestTriggering) {
      if (!configuration.requestTriggerEndpoints.isEmpty()) {
        alertingSubsystem.setEnableRequestTriggerUpdates(false);
        logger.info(
            "Request Trigger configuration has been provided in settings, trigger settings provided via the Portal UI will be ignored");
      }

      List<AlertPipeline> spanPipelines =
          configuration.requestTriggerEndpoints.stream()
              .map(it -> RequestAlertPipelineBuilder.build(it, alertAction, TimeSource.DEFAULT))
              .collect(Collectors.toList());

      alertingSubsystem.setPipeline(
          AlertMetricType.REQUEST, new AlertPipelineMultiplexer(spanPipelines));
    }

    addObserver(alertingSubsystem, telemetryObservers);

    GcEventInit.init(
        alertingSubsystem,
        telemetryClient,
        executorService,
        fromGcEventMonitorConfiguration(reportingLevel));

    return alertingSubsystem;
  }

  private static GcEventInit.GcEventMonitorConfiguration fromGcEventMonitorConfiguration(
      GcReportingLevel reportingLevel) {
    if (reportingLevel != null) {
      return new GcEventInit.GcEventMonitorConfiguration(reportingLevel);
    }

    return new GcEventInit.GcEventMonitorConfiguration(GcReportingLevel.NONE);
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

  private static void alertAction(
      AlertBreach alert,
      Profiler profiler,
      DiagnosticEngine diagnosticEngine,
      TelemetryClient telemetryClient) {

    if (profiler != null) {
      // This is an event that the backend specifically looks for to track when a profile is
      // started
      sendMessageTelemetry(telemetryClient, "StartProfiler triggered.");

      profiler.accept(
          alert,
          serviceProfilerIndex -> sendServiceProfilerIndex(serviceProfilerIndex, telemetryClient));

      if (diagnosticEngine != null) {
        diagnosticEngine.performDiagnosis(alert);
      }
    }
  }

  private static void sendServiceProfilerIndex(
      ServiceProfilerIndex serviceProfilerIndex, TelemetryClient telemetryClient) {

    EventTelemetryBuilder telemetryBuilder = telemetryClient.newEventTelemetryBuilder();

    telemetryBuilder.setName("ServiceProfilerIndex");

    for (Map.Entry<String, String> entry : serviceProfilerIndex.getProperties().entrySet()) {
      telemetryBuilder.addProperty(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, Double> entry : serviceProfilerIndex.getMetrics().entrySet()) {
      telemetryBuilder.addMeasurement(entry.getKey(), entry.getValue());
    }

    telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());

    telemetryClient.trackAsync(telemetryBuilder.build());

    // This is an event that the backend specifically looks for to track when a profile is
    // complete
    sendMessageTelemetry(telemetryClient, "StopProfiler succeeded.");
  }

  private static void sendMessageTelemetry(TelemetryClient telemetryClient, String message) {
    MessageTelemetryBuilder telemetryBuilder = telemetryClient.newMessageTelemetryBuilder();

    telemetryBuilder.setMessage(message);
    telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());

    telemetryClient.trackAsync(telemetryBuilder.build());
  }

  static AlertingSubsystem getAlertingSubsystem() {
    return alertingSubsystem;
  }

  private AlertingSubsystemInit() {}
}
