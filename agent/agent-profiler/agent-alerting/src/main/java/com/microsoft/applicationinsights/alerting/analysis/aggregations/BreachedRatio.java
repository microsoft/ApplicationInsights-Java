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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

public class BreachedRatio {

  private final long windowLengthInSec;
  private final TimeSource timeSource;
  private final long minimumSamples;

  private static class Bucket {
    final Instant bucketStart;
    int totalCount = 0;
    int breachedCount = 0;

    private Bucket(Instant bucketStart) {
      this.bucketStart = bucketStart;
    }

    public void increment(boolean breached) {
      if (breached) {
        breachedCount++;
      }
      totalCount++;
    }
  }

  private final Object bucketLock = new Object();
  private final List<Bucket> buckets = Collections.synchronizedList(new ArrayList<>());

  public OptionalDouble update(boolean breached) {
    getBucket().increment(breached);

    return calculateRatio();
  }

  public OptionalDouble calculateRatio() {
    Instant now = timeSource.getNow();
    Instant cutoff = now.minusSeconds(windowLengthInSec);
    gcBuckets(cutoff);

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

  private Bucket getBucket() {
    synchronized (bucketLock) {
      Instant now = timeSource.getNow();
      Instant cutoff = now.minusSeconds(windowLengthInSec);
      gcBuckets(cutoff);

      if (buckets.isEmpty()) {
        buckets.add(new Bucket(now));
      }

      Bucket last = buckets.get(buckets.size() - 1);
      if (last.bucketStart.isBefore(now.minusSeconds(1))) {
        last = new Bucket(now);
        buckets.add(last);
      }

      return last;
    }
  }

  private void gcBuckets(Instant cutoff) {
    while (buckets.size() > 0 && buckets.get(0).bucketStart.isBefore(cutoff)) {
      buckets.remove(0);
    }
  }

  public BreachedRatio(long windowLengthInSec, long minimumSamples, TimeSource timeSource) {
    this.windowLengthInSec = windowLengthInSec;
    this.timeSource = timeSource;
    this.minimumSamples = minimumSamples;
  }
}
