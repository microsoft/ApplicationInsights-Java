package com.microsoft.applicationinsights.alerting.analysis.aggregations.windowed;

import java.time.Instant;

class WindowedAggregationBucket<T extends BucketData<U>, U> {
  final Instant bucketEnd;
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
}
