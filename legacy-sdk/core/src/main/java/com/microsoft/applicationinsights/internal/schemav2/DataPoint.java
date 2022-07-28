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

public class DataPoint {

  private String name;
  private DataPointType kind = DataPointType.Measurement;
  private double value;
  private Integer count;
  private Double min;
  private Double max;
  private Double stdDev;

  public DataPoint() {}

  public String getName() {
    return this.name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public DataPointType getKind() {
    return this.kind;
  }

  public void setKind(DataPointType value) {
    this.kind = value;
  }

  public double getValue() {
    return this.value;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public Integer getCount() {
    return this.count;
  }

  public void setCount(Integer value) {
    this.count = value;
  }

  public Double getMin() {
    return this.min;
  }

  public void setMin(Double value) {
    this.min = value;
  }

  public Double getMax() {
    return this.max;
  }

  public void setMax(Double value) {
    this.max = value;
  }

  public Double getStdDev() {
    return this.stdDev;
  }

  public void setStdDev(Double value) {
    this.stdDev = value;
  }
}
