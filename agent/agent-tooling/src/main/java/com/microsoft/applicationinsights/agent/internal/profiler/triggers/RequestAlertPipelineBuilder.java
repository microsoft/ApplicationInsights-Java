// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
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
  public static AlertPipeline build(
      Configuration.RequestTrigger configuration,
      Consumer<AlertBreach> alertAction,
      TimeSource timeSource) {

    AlertingConfig.RequestTrigger requestTriggerConfiguration =
        buildRequestTriggerConfiguration(configuration);

    AlertRequestFilter filter = AlertRequestFilterBuilder.build(configuration.filter);

    Aggregation aggregation = getAggregation(configuration, timeSource);

    // TODO make threshold and throttling responsive to type argument
    AlertingConfiguration.AlertConfiguration config =
        new AlertingConfiguration.AlertConfiguration(
            AlertMetricType.REQUEST,
            true,
            configuration.threshold.value,
            configuration.profileDuration,
            configuration.throttling.value,
            requestTriggerConfiguration);

    return SingleAlertPipeline.create(filter, aggregation, config, alertAction);
  }

  public static AlertingConfig.RequestTrigger buildRequestTriggerConfiguration(
      Configuration.RequestTrigger configuration) {

    AlertingConfig.RequestTriggerType type =
        AlertingConfig.RequestTriggerType.valueOf(configuration.type.name());

    AlertingConfig.RequestFilter filter =
        new AlertingConfig.RequestFilter(
            AlertingConfig.RequestFilterType.valueOf(configuration.filter.type.name()),
            configuration.filter.value);

    AlertingConfig.RequestAggregationConfig requestAggregationConfig =
        new AlertingConfig.RequestAggregationConfig(
            configuration.aggregation.configuration.thresholdMillis,
            configuration.aggregation.configuration.minimumSamples);

    AlertingConfig.RequestAggregation aggregation =
        new AlertingConfig.RequestAggregation(
            AlertingConfig.RequestAggregationType.valueOf(configuration.aggregation.type.name()),
            configuration.aggregation.windowSizeMillis,
            requestAggregationConfig);

    AlertingConfig.RequestTriggerThreshold requestTriggerThreshold =
        new AlertingConfig.RequestTriggerThreshold(
            AlertingConfig.RequestTriggerThresholdType.valueOf(configuration.threshold.type.name()),
            configuration.threshold.value);

    AlertingConfig.RequestTriggerThrottling throttling =
        new AlertingConfig.RequestTriggerThrottling(
            AlertingConfig.RequestTriggerThrottlingType.valueOf(
                configuration.throttling.type.name()),
            configuration.throttling.value);

    return new AlertingConfig.RequestTrigger(
        configuration.name,
        type,
        filter,
        aggregation,
        requestTriggerThreshold,
        throttling,
        configuration.profileDuration);
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
