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
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Applies a time window to data held in 1 second buckets. I.e holds the last n seconds of data. */
public class WindowedAggregation<T extends WindowedAggregation.BucketData<U>, U> {
  private static final int DEFAULT_WINDOW_IN_SEC =
      Integer.parseInt(
          System.getProperty("applicationinsights.preview.profiler.rolling-window-in-sec", "120"));
  private final long windowLengthInSec;
  private final TimeSource timeSource;

  private final Object bucketLock = new Object();
  private final List<Bucket> buckets = Collections.synchronizedList(new ArrayList<>());
  private final Supplier<T> bucketFactory;

  public WindowedAggregation(Supplier<T> bucketFactory) {
    this(DEFAULT_WINDOW_IN_SEC, TimeSource.DEFAULT, bucketFactory);
  }

  public WindowedAggregation(
      long windowLengthInSec, TimeSource timeSource, Supplier<T> bucketFactory) {
    this.windowLengthInSec = windowLengthInSec;
    this.timeSource = timeSource;
    this.bucketFactory = bucketFactory;
  }

  public interface BucketData<U> {
    void update(U sample);
  }

  private class Bucket {
    final Instant bucketStart;
    private final T data;

    private Bucket(Instant bucketStart, T data) {
      this.bucketStart = bucketStart;
      this.data = data;
    }

    public void update(U newSample) {
      data.update(newSample);
    }
  }

  public void update(U breached) {
    getBucket().update(breached);
  }

  public List<T> getData() {
    return buckets.stream().map(it -> it.data).collect(Collectors.toList());
  }

  private Bucket getBucket() {
    synchronized (bucketLock) {
      Instant now = timeSource.getNow();
      Instant cutoff = now.minusSeconds(windowLengthInSec);
      gcBuckets(cutoff);

      if (buckets.isEmpty()) {
        buckets.add(new Bucket(now, bucketFactory.get()));
      }

      Bucket last = buckets.get(buckets.size() - 1);
      if (last.bucketStart.isBefore(now.minusSeconds(1))) {
        last = new Bucket(now, bucketFactory.get());
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
}
