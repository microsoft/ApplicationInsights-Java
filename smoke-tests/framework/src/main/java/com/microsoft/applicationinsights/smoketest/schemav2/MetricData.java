// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Data contract class MetricData. */
public class MetricData extends Domain {
  /** Backing field for property Metrics. */
  private List<DataPoint> metrics;

  /** Backing field for property Properties. */
  private ConcurrentMap<String, String> properties;

  /** Initializes a new instance of the MetricData class. */
  public MetricData() {}

  /** Gets the Metrics property. */
  public List<DataPoint> getMetrics() {
    if (this.metrics == null) {
      this.metrics = new ArrayList<>();
    }
    return this.metrics;
  }

  /** Gets the Properties property. */
  public ConcurrentMap<String, String> getProperties() {
    if (this.properties == null) {
      this.properties = new ConcurrentHashMap<>();
    }
    return this.properties;
  }
}
