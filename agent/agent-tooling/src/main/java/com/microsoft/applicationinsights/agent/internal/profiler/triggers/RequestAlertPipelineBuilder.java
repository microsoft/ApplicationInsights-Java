// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.Aggregation;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.ThresholdBreachRatioAggregation;
import com.microsoft.applicationinsights.alerting.analysis.filter.AlertRequestFilter;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipeline;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.SingleAlertPipeline;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/** Constructs an AlertPipeline for processing span telemetry data. */
public class RequestAlertPipelineBuilder {

  private RequestAlertPipelineBuilder() {}

  /** Form a single trigger context from configuration. */
  @Nullable
  public static AlertPipeline build(
      Configuration.RequestTrigger configuration,
      Consumer<AlertBreach> alertAction,
      TimeSource timeSource) {

    AlertRequestFilter filter = AlertRequestFilterBuilder.build(configuration.filter);

    Aggregation aggregation = getAggregation(configuration, timeSource);

    // TODO make threshold and throttling responsive to type argument
    AlertingConfiguration.AlertConfiguration config =
        new AlertingConfiguration.AlertConfiguration(
            AlertMetricType.REQUEST,
            true,
            configuration.threshold.value,
            configuration.profileDuration,
            configuration.throttling.value);

    return SingleAlertPipeline.create(filter, aggregation, config, alertAction);
  }

  @Nullable
  private static Aggregation getAggregation(
      Configuration.RequestTrigger configuration, TimeSource timeSource) {
    if (configuration.aggregation.type == Configuration.RequestAggregationType.BREACH_RATIO) {
      return new ThresholdBreachRatioAggregation(
          configuration.aggregation.configuration.thresholdMillis,
          configuration.aggregation.configuration.minimumSamples,
          configuration.aggregation.windowSizeMillis / 1000,
          timeSource,
          false);
    }
    return null;
  }
}
