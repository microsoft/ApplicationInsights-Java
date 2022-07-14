package com.microsoft.applicationinsights.alerting.analysis.aggregations.windowed;

public interface BucketData<U> {
  void update(U sample);
}