// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.aggregations.windowed;

/** A data type that can be aggregated by WindowedAggregation */
public interface BucketData<U> {
  /** Applies new sample to buckets data */
  void update(U sample);
}
