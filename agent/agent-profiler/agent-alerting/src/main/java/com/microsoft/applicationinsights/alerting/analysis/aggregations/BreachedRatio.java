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

  private static class BreachedCountBucket implements WindowedAggregation.BucketData<Boolean> {
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
