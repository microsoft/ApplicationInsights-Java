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
