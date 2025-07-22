// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.pipelines;

import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.RollingAverage;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.ThresholdBreachRatioAggregation;
import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import com.microsoft.applicationinsights.alerting.analysis.filter.AlertRequestFilter;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains analysis pipelines for all metric types. */
public class AlertPipelines {
  private static final Logger logger = LoggerFactory.getLogger(AlertPipelines.class);

  // List of alert analysis pipelines for each metric type, entrypoint
  // for the pipeline is a rolling average
  private final Map<AlertMetricType, AlertPipeline> alertPipelines = new HashMap<>();

  // Handler to notify when a breach happens
  private final Consumer<AlertBreach> alertHandler;

  public AlertPipelines(Consumer<AlertBreach> alertHandler) {
    this.alertHandler = alertHandler;
  }

  public OptionalDouble getAverage(AlertMetricType type) {
    AlertPipeline pipeline = alertPipelines.get(type);
    if (pipeline != null) {
      return pipeline.getValue();
    } else {
      return OptionalDouble.empty();
    }
  }

  public void updateAlertConfig(AlertConfiguration newAlertConfig, TimeSource timeSource) {
    AlertPipeline pipeline = alertPipelines.get(newAlertConfig.getType());
    if (pipeline == null) {
      pipeline =
          SingleAlertPipeline.create(
              new AlertRequestFilter.AcceptAll(),
              new RollingAverage(120, timeSource, true),
              newAlertConfig,
              this::dispatchAlert);
      alertPipelines.put(newAlertConfig.getType(), pipeline);
    } else {
      pipeline.updateConfig(newAlertConfig);
    }

    logger.debug(
        "Set alert configuration for {}: {}", newAlertConfig.getType(), newAlertConfig.toString());
  }

  public void updateRequestAlertConfig(
      List<AlertConfiguration> newAlertConfig, TimeSource timeSource) {
    List<AlertPipeline> requestPipelines =
        newAlertConfig.stream()
            .map(
                alert -> {
                  AlertingConfig.RequestTrigger trigger = alert.getRequestTrigger();

                  return SingleAlertPipeline.create(
                      new AlertRequestFilter.RegexRequestNameFilter(trigger.filter.value),
                      new ThresholdBreachRatioAggregation(
                          trigger.aggregation.configuration.thresholdMillis,
                          trigger.aggregation.configuration.minimumSamples,
                          trigger.aggregation.windowSizeMillis / 1000,
                          timeSource,
                          false),
                      alert,
                      alertHandler);
                })
            .collect(Collectors.toList());

    alertPipelines.put(AlertMetricType.REQUEST, new AlertPipelineMultiplexer(requestPipelines));

    logger.debug(
        "Set alert configuration for {}: {} pipelines updated",
        AlertMetricType.REQUEST,
        newAlertConfig.size());
  }

  /** Ensure that alerts contain the required metrics and notify upstream handler. */
  private void dispatchAlert(AlertBreach alert) {
    alertHandler.accept(addMetricData(alert));
  }

  public void setAlertPipeline(AlertMetricType type, AlertPipeline alertPipeline) {
    alertPipelines.put(type, alertPipeline);
  }

  // Ensure that cpu and memory values are set on the breach
  private AlertBreach addMetricData(AlertBreach alert) {
    if (alert.getMemoryUsage() == 0.0) {
      OptionalDouble memory = getAverage(AlertMetricType.MEMORY);
      if (memory.isPresent()) {
        alert = alert.toBuilder().setMemoryUsage(memory.getAsDouble()).build();
      }
    }

    if (alert.getCpuMetric() == 0.0) {
      OptionalDouble cpu = getAverage(AlertMetricType.CPU);
      if (cpu.isPresent()) {
        alert = alert.toBuilder().setCpuMetric(cpu.getAsDouble()).build();
      }
    }
    return alert;
  }

  /** Route telemetry to the appropriate pipeline. */
  public void process(TelemetryDataPoint telemetryDataPoint) {
    AlertPipeline pipeline = alertPipelines.get(telemetryDataPoint.getType());
    if (pipeline != null) {
      pipeline.track(telemetryDataPoint);
    }
  }
}
