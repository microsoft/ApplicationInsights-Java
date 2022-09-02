// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.schemav2;

import javax.annotation.Nullable;

public class DataPoint {

  private String name;
  private String metricNamespace;
  private DataPointType kind = DataPointType.Measurement;
  private double value;
  @Nullable private Integer count;
  @Nullable private Double min;
  @Nullable private Double max;
  @Nullable private Double stdDev;

  public DataPoint() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMetricNamespace() {
    return metricNamespace;
  }

  public void setMetricNamespace(String metricNamespace) {
    this.metricNamespace = metricNamespace;
  }

  public DataPointType getKind() {
    return kind;
  }

  public void setKind(DataPointType kind) {
    this.kind = kind;
  }

  public double getValue() {
    return value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  @Nullable
  public Integer getCount() {
    return count;
  }

  public void setCount(@Nullable Integer count) {
    this.count = count;
  }

  @Nullable
  public Double getMin() {
    return min;
  }

  public void setMin(@Nullable Double min) {
    this.min = min;
  }

  @Nullable
  public Double getMax() {
    return max;
  }

  public void setMax(@Nullable Double max) {
    this.max = max;
  }

  @Nullable
  public Double getStdDev() {
    return stdDev;
  }

  public void setStdDev(@Nullable Double stdDev) {
    this.stdDev = stdDev;
  }
}
