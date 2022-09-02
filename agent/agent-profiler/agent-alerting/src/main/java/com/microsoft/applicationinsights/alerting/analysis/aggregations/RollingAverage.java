// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.aggregations;

import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.windowed.BucketData;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.windowed.WindowedAggregation;
import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import java.util.List;
import java.util.OptionalDouble;

/** Applies a time window to data and calculates a mean of the data during that window. */
public class RollingAverage extends Aggregation {
  private final WindowedAggregation<RollingAverageSample, TelemetryDataPoint> windowedAggregation;

  public RollingAverage(long windowLengthInSec, TimeSource timeSource, boolean trackCurrentBucket) {
    windowedAggregation =
        new WindowedAggregation<>(
            windowLengthInSec, timeSource, RollingAverageSample::new, trackCurrentBucket);
  }

  private static class RollingAverageSample implements BucketData<TelemetryDataPoint> {
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
