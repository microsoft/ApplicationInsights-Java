// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.aggregations;

import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import java.util.OptionalDouble;

public class ThresholdBreachRatioAggregation extends Aggregation {

  private final BreachedRatio breachRatio;
  private final double thresholdMillis;

  public ThresholdBreachRatioAggregation(
      long thresholdMillis,
      long minimumSamples,
      long windowLengthInSec,
      TimeSource timeSource,
      boolean trackCurrentBucket) {
    this.breachRatio =
        new BreachedRatio(windowLengthInSec, minimumSamples, timeSource, trackCurrentBucket);
    this.thresholdMillis = thresholdMillis;
  }

  @Override
  public void processUpdate(TelemetryDataPoint telemetryDataPoint) {
    this.breachRatio.update(telemetryDataPoint.getValue() >= thresholdMillis);
  }

  @Override
  public OptionalDouble compute() {
    return this.breachRatio.calculateRatio();
  }
}
