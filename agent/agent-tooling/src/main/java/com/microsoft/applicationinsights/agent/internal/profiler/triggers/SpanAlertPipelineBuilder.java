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

package com.microsoft.applicationinsights.agent.internal.profiler.triggers;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.Aggregation;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.ThresholdBreachRatioAggregation;
import com.microsoft.applicationinsights.alerting.analysis.filter.AlertSpanFilter;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipeline;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.SingleAlertPipeline;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/** Constructs an AlertPipeline for processing span telemetry data. */
public class SpanAlertPipelineBuilder {

  private SpanAlertPipelineBuilder() {}

  /** Form a single trigger context from configuration. */
  @Nullable
  public static AlertPipeline build(
      Configuration.SpanTrigger configuration,
      Consumer<AlertBreach> alertAction,
      TimeSource timeSource) {

    AlertSpanFilter filter = AlertSpanFilterBuilder.build(configuration.filter);

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
      Configuration.SpanTrigger configuration, TimeSource timeSource) {
    if (configuration.aggregation.type == Configuration.SpanAggregationType.BREACH_RATIO) {
      return new ThresholdBreachRatioAggregation(
          configuration.aggregation.configuration.thresholdMs,
          configuration.aggregation.configuration.minimumSamples,
          configuration.aggregation.windowSize / 1000,
          timeSource);
    }
    return null;
  }
}
