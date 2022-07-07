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

package com.microsoft.applicationinsights.alerting.analysis.aggregations;

import com.google.common.math.Quantiles;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import java.util.OptionalDouble;
import java.util.function.DoubleConsumer;

/** Applies a time window to data and calculates a quantile of the data during that window. */
public class QuantileAggregation implements Aggregation {

  private final int quantile;
  private DoubleConsumer consumer;
  private final WindowedAggregation windowedAggregation;

  /** Quantile is the quantile to calculate, i.e 95 would calculate the 95th quantile. */
  public QuantileAggregation(int quantile) {
    windowedAggregation = new WindowedAggregation();
    this.quantile = quantile;
  }

  public QuantileAggregation(int quantile, long windowLengthInSec) {
    this(quantile, windowLengthInSec, TimeSource.DEFAULT);
  }

  public QuantileAggregation(int quantile, long windowLengthInSec, TimeSource timeSource) {
    windowedAggregation = new WindowedAggregation(windowLengthInSec, timeSource);
    this.quantile = quantile;
  }

  @Override
  public void setConsumer(DoubleConsumer consumer) {
    this.consumer = consumer;
  }

  @Override
  public double update(TelemetryDataPoint telemetryDataPoint) {

    windowedAggregation.update(telemetryDataPoint);

    OptionalDouble average = compute();
    if (average.isPresent()) {
      consumer.accept(average.getAsDouble());
      return average.getAsDouble();
    } else {
      return 0.0d;
    }
  }

  @Override
  public OptionalDouble compute() {
    double[] dataPoints =
        windowedAggregation.getTelemetryDataPoints().stream()
            .mapToDouble(TelemetryDataPoint::getValue)
            .toArray();

    if (dataPoints.length == 0) {
      return OptionalDouble.empty();
    }

    return OptionalDouble.of(Quantiles.percentiles().index(quantile).computeInPlace(dataPoints));
  }
}
