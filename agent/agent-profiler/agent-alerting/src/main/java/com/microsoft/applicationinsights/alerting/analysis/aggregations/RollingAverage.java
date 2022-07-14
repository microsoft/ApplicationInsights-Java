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
import java.util.List;
import java.util.OptionalDouble;

/** Applies a time window to data and calculates a mean of the data during that window. */
public class RollingAverage extends Aggregation {
  private final WindowedAggregation<RollingAverageSample, TelemetryDataPoint> windowedAggregation;

  public RollingAverage(TimeSource timeSource, boolean trackCurrentBucket) {
    windowedAggregation =
        new WindowedAggregation<>(timeSource, RollingAverageSample::new, trackCurrentBucket);
  }

  public RollingAverage(long windowLengthInSec, TimeSource timeSource, boolean trackCurrentBucket) {
    windowedAggregation =
        new WindowedAggregation<>(
            windowLengthInSec, timeSource, RollingAverageSample::new, trackCurrentBucket);
  }

  private static class RollingAverageSample
      implements WindowedAggregation.BucketData<TelemetryDataPoint> {
    int sampleCount = 0;
    double totalTime = 0;

    @Override
    public void update(TelemetryDataPoint data) {
      totalTime += data.getValue();
      sampleCount++;
    }
  }

  @Override
  public void processUpdate(TelemetryDataPoint telemetryDataPoint) {
    windowedAggregation.update(telemetryDataPoint);
  }

  @Override
  public OptionalDouble compute() {
    List<RollingAverageSample> data = windowedAggregation.getData();
    long count = data.stream().mapToLong(it -> it.sampleCount).sum();

    if (count == 0) {
      return OptionalDouble.empty();
    }

    double totalTime = data.stream().mapToDouble(it -> it.totalTime).sum();

    return OptionalDouble.of(totalTime / (double) count);
  }
}
