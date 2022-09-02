// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.aggregations.windowed;

import java.time.Instant;

/** Wraps data held within the WindowedAggregation */
class WindowedAggregationBucket<T extends BucketData<U>, U> {
  /** Time after which this bucket will be considered complete */
  private final Instant bucketEnd;

  /** Mutable bucket for accumulating data within the bucket */
  private final T data;

  WindowedAggregationBucket(Instant bucketEnd, T data) {
    this.bucketEnd = bucketEnd;
    this.data = data;
  }

  public void update(U newSample) {
    data.update(newSample);
  }

  public T getData() {
    return data;
  }

  public Instant getBucketEnd() {
    return bucketEnd;
  }
}
