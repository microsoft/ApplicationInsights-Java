// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.pipelines;

import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import java.util.List;
import java.util.OptionalDouble;

/** Distributes telemetry to multiple downstream AlertPipelines. */
public class AlertPipelineMultiplexer implements AlertPipeline {

  private final List<AlertPipeline> pipelines;

  public AlertPipelineMultiplexer(List<AlertPipeline> pipelines) {
    this.pipelines = pipelines;
  }

  @Override
  public OptionalDouble getValue() {
    return OptionalDouble.empty();
  }

  @Override
  public void updateConfig(AlertConfiguration newAlertConfig) {}

  @Override
  public void track(TelemetryDataPoint telemetryDataPoint) {
    pipelines.forEach(it -> it.track(telemetryDataPoint));
  }
}
