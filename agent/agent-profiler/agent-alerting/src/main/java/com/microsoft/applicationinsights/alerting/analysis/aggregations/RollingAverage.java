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

import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import java.util.OptionalDouble;
import java.util.function.Consumer;

/** Applies a time window to data and calculates a mean of the data during that window. */
public class RollingAverage implements Aggregation {
  private Consumer<Double> consumer;
  private final WindowedAggregation windowedAggregation;

  public RollingAverage() {
    windowedAggregation = new WindowedAggregation();
  }

  public RollingAverage(long windowLengthInSec) {
    this(windowLengthInSec, TimeSource.DEFAULT);
  }

  public RollingAverage(long windowLengthInSec, TimeSource timeSource) {
    windowedAggregation = new WindowedAggregation(windowLengthInSec, timeSource);
  }

  @Override
  public void setConsumer(Consumer<Double> consumer) {
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
    return windowedAggregation.getTelemetryDataPoints().stream()
        .mapToDouble(TelemetryDataPoint::getValue)
        .average();
  }
}
