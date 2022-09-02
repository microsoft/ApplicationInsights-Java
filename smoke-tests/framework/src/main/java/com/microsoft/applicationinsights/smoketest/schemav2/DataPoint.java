// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

/** Data contract class DataPoint. */
public class DataPoint {
  /** Backing field for property Name. */
  private String name;

  /** Backing field for property Namespace. */
  private String ns;

  /** Backing field for property Value. */
  private double value;

  /** Backing field for property Count. */
  private Integer count;

  /** Backing field for property Min. */
  private Double min;

  /** Backing field for property Max. */
  private Double max;

  /** Backing field for property StdDev. */
  private Double stdDev;

  /** Initializes a new instance of the DataPoint class. */
  public DataPoint() {}

  /** Gets the Name property. */
  public String getName() {
    return this.name;
  }

  /** Sets the Name property. */
  public void setName(String value) {
    this.name = value;
  }

  /** Gets the Namespace property. */
  public String getMetricNamespace() {
    return this.ns;
  }

  /** Sets the MetricNamespace property. */
  public void setMetricNamespace(String value) {
    this.ns = value;
  }

  /** Gets the Value property. */
  public double getValue() {
    return this.value;
  }

  /** Sets the Value property. */
  public void setValue(double value) {
    this.value = value;
  }

  /** Gets the Count property. */
  public Integer getCount() {
    return this.count;
  }

  /** Sets the Count property. */
  public void setCount(Integer value) {
    this.count = value;
  }

  /** Gets the Min property. */
  public Double getMin() {
    return this.min;
  }

  /** Sets the Min property. */
  public void setMin(Double value) {
    this.min = value;
  }

  /** Gets the Max property. */
  public Double getMax() {
    return this.max;
  }

  /** Sets the Max property. */
  public void setMax(Double value) {
    this.max = value;
  }

  /** Gets the StdDev property. */
  public Double getStdDev() {
    return this.stdDev;
  }

  /** Sets the StdDev property. */
  public void setStdDev(Double value) {
    this.stdDev = value;
  }
}
