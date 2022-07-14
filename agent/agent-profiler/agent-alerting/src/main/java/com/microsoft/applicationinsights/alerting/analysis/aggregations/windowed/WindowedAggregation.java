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

package com.microsoft.applicationinsights.alerting.analysis.aggregations.windowed;

import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Applies a time window to data held in 1 second buckets. I.e holds the last n seconds of data. */
public class WindowedAggregation<T extends BucketData<U>, U> {
  public static final int BUCKET_DURATION_SECONDS = 2;
  private final long windowLengthInSec;
  private final TimeSource timeSource;

  private final Object bucketLock = new Object();
  private final List<WindowedAggregationBucket<T, U>> buckets =
      Collections.synchronizedList(new ArrayList<>());
  private WindowedAggregationBucket<T, U> currentBucket;
  private final Supplier<T> bucketFactory;

  // Determines if the current bucket that is in the process of being calculated is included
  // in the returned data
  private final boolean trackCurrentBucket;

  public WindowedAggregation(
      long windowLengthInSec,
      TimeSource timeSource,
      Supplier<T> bucketFactory,
      boolean trackCurrentBucket) {
    this.windowLengthInSec = windowLengthInSec;
    this.timeSource = timeSource;
    this.bucketFactory = bucketFactory;
    this.trackCurrentBucket = trackCurrentBucket;
  }

  public void update(U breached) {
    getBucket().update(breached);
  }

  public List<T> getData() {
    Instant now = timeSource.getNow();
    Instant cutoff = now.minusSeconds(windowLengthInSec);
    gcBuckets(cutoff);
    return buckets.stream().map(WindowedAggregationBucket::getData).collect(Collectors.toList());
  }

  private WindowedAggregationBucket<T, U> getBucket() {
    synchronized (bucketLock) {
      Instant now = timeSource.getNow();

      if (currentBucket == null) {
        currentBucket =
            new WindowedAggregationBucket<>(
                now.plusSeconds(BUCKET_DURATION_SECONDS), bucketFactory.get());
        if (trackCurrentBucket) {
          buckets.add(currentBucket);
        }
      }

      if (currentBucket.bucketEnd.isBefore(now)) {
        // Gone past end of current bucket, add it to the array
        Instant cutoff = now.minusSeconds(windowLengthInSec);
        gcBuckets(cutoff);

        if (!trackCurrentBucket) {
          buckets.add(currentBucket);
        }

        currentBucket =
            new WindowedAggregationBucket<>(
                now.plusSeconds(BUCKET_DURATION_SECONDS), bucketFactory.get());

        if (trackCurrentBucket) {
          buckets.add(currentBucket);
        }
      }

      return currentBucket;
    }
  }

  private void gcBuckets(Instant cutoff) {
    synchronized (bucketLock) {
      while (buckets.size() > 0 && buckets.get(0).bucketEnd.isBefore(cutoff)) {
        buckets.remove(0);
      }
    }
  }
}
