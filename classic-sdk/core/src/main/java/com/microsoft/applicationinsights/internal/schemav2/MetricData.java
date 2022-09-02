// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.schemav2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MetricData {

  private List<DataPoint> metrics;
  private ConcurrentMap<String, String> properties;

  public MetricData() {}

  public List<DataPoint> getMetrics() {
    if (metrics == null) {
      metrics = new ArrayList<>();
    }
    return metrics;
  }

  public void setMetrics(List<DataPoint> metrics) {
    this.metrics = metrics;
  }

  public ConcurrentMap<String, String> getProperties() {
    if (properties == null) {
      properties = new ConcurrentHashMap<>();
    }
    return properties;
  }

  public void setProperties(ConcurrentMap<String, String> properties) {
    this.properties = properties;
  }
}
