// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.aggregations;

import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.windowed.BucketData;
import com.microsoft.applicationinsights.alerting.analysis.aggregations.windowed.WindowedAggregation;
import java.util.List;
import java.util.OptionalDouble;

public class BreachedRatio {

  private final long minimumSamples;

  private final WindowedAggregation<BreachedCountBucket, Boolean> windowedAggregation;

  public BreachedRatio(
      long windowLengthInSec,
      long minimumSamples,
      TimeSource timeSource,
      boolean trackCurrentBucket) {
    this.windowedAggregation =
        new WindowedAggregation<>(
            windowLengthInSec, timeSource, BreachedCountBucket::new, trackCurrentBucket);
    this.minimumSamples = minimumSamples;
  }

  private static class BreachedCountBucket implements BucketData<Boolean> {
    int totalCount = 0;
    int breachedCount = 0;

    @Override
    public void update(Boolean breached) {
      if (breached) {
        breachedCount++;
      }
      totalCount++;
    }
  }

  public void update(boolean breached) {
    windowedAggregation.update(breached);
  }

  public OptionalDouble calculateRatio() {
    List<BreachedCountBucket> buckets = windowedAggregation.getData();

    if (buckets.isEmpty()) {
      return OptionalDouble.empty();
    }

    int total = buckets.stream().mapToInt(it -> it.totalCount).sum();

    if (total < minimumSamples) {
      return OptionalDouble.empty();
    }

    int breached = buckets.stream().mapToInt(it -> it.breachedCount).sum();

    if (total == 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of((double) breached / (double) total);
  }
}
