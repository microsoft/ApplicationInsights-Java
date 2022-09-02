// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;

/**
 * Telemetry type used to track metrics sent to Azure Application Insights.
 *
 * <p>This represents a Measurement, if only Name and Value are set. If Count, Min, Max or Standard
 * Deviation are set, this represents an Aggregation; a sampled set of points summarized by these
 * statistic fields. In an Aggregation metric, the value, i.e. {@link #getValue()}, represents the
 * sum of sampled data points.
 */
public final class MetricTelemetry extends BaseTelemetry {

  private final MetricData data;
  private final DataPoint metric;

  /**
   * Creates a new instance.
   *
   * @param name The name of the metric. Length 1-150 characters.
   * @param value The value of the metric.
   * @throws IllegalArgumentException if name is null or empty
   */
  public MetricTelemetry(String name, double value) {
    this();
    setName(name);
    metric.setValue(value);
  }

  /** Creates a new instance. */
  public MetricTelemetry() {
    data = new MetricData();
    metric = new DataPoint();
    initialize(data.getProperties());
    data.getMetrics().add(metric);
  }

  /** Gets the name of the metric. */
  public String getName() {
    return metric.getName();
  }

  /**
   * Sets the name of the metric. Length 1-150 characters.
   *
   * @param name The name of the metric.
   * @throws IllegalArgumentException if the name is null or empty.
   */
  public void setName(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("The metric name cannot be null or empty");
    }
    metric.setName(name);
  }

  /** Gets the namespace of the metric. */
  public String getMetricNamespace() {
    return metric.getMetricNamespace();
  }

  /**
   * Sets the metric namespace for the metric. This is optional.
   *
   * @param metricNamespace The metric namespace for the metric.
   */
  public void setMetricNamespace(String metricNamespace) {
    metric.setMetricNamespace(metricNamespace);
  }

  /**
   * Gets the value of the metric. Represents the sum of data points if this metric is an
   * Aggregation.
   */
  public double getValue() {
    return metric.getValue();
  }

  /** Sets The value of the metric. */
  public void setValue(double value) {
    metric.setValue(value);
  }

  /** Gets the number of samples for this metric. */
  public Integer getCount() {
    return metric.getCount();
  }

  /**
   * Sets the number of samples for this metric.
   *
   * @param count Number of samples greater than or equal to 1
   */
  public void setCount(Integer count) {
    metric.setCount(count);
    updateKind();
  }

  /** Gets the min value of this metric across samples. */
  public Double getMin() {
    return metric.getMin();
  }

  /** Sets the min value of this metric across samples. */
  public void setMin(Double value) {
    metric.setMin(value);
    updateKind();
  }

  /** Gets the max value of this metric across samples. */
  public Double getMax() {
    return metric.getMax();
  }

  /** Sets the max value of this metric across samples. */
  public void setMax(Double value) {
    metric.setMax(value);
    updateKind();
  }

  /** Gets the standard deviation of this metric across samples. */
  public Double getStandardDeviation() {
    return metric.getStdDev();
  }

  /** Sets the standard deviation of this metric across samples. */
  public void setStandardDeviation(Double value) {
    metric.setStdDev(value);
    updateKind();
  }

  private void updateKind() {
    // if any stats are set, assume it's an aggregation.
    boolean isAggregation =
        (metric.getCount() != null)
            || (metric.getMin() != null)
            || (metric.getMax() != null)
            || (metric.getStdDev() != null);

    metric.setKind(isAggregation ? DataPointType.Aggregation : DataPointType.Measurement);
  }

  public DataPointType getKind() {
    return metric.getKind();
  }

  @Override
  protected MetricData getData() {
    return data;
  }
}
