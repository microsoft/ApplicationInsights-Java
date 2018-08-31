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

package com.microsoft.applicationinsights.telemetry;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.schemav2.DataPoint;
import com.microsoft.applicationinsights.internal.schemav2.DataPointType;
import com.microsoft.applicationinsights.internal.schemav2.MetricData;
import com.microsoft.applicationinsights.internal.util.Sanitizer;

/**
 * Telemetry type used to track metrics sent to Azure Application Insights.
 * <p>
 * This represents a Measurement, if only Name and Value are set.
 * If Count, Min, Max or Standard Deviation are set, this represents an Aggregation;
 * a sampled set of points summarized by these statistic fields.
 * In an Aggregation metric, the value, i.e. {@link #getValue()}, represents the sum of sampled data points.
 * </p>
 */
public final class MetricTelemetry extends BaseTelemetry<MetricData> {
    private final MetricData data;
    private final DataPoint metric;

    /**
     * Envelope Name for this telemetry.
     */
    private static final String ENVELOPE_NAME = "Metric";


    /**
     * Base Type for this telemetry.
     */
    public static final String BASE_TYPE = "MetricData";

    /**
     * Default constructor
     */
    public MetricTelemetry() {
        super();
        data = new MetricData();
        metric = new DataPoint();
        initialize(data.getProperties());
        data.getMetrics().add(metric);
    }

    @Override
    public int getVer() {
        return getData().getVer();
    }

    /**
     * Initializes the instance with a name and value
     * @param name The name of the metric. Length 1-150 characters.
     * @param value The value of the metric.
     * @throws IllegalArgumentException if name is null or empty
     */
    public MetricTelemetry(String name, double value) {
        this();
        setName(name);
        metric.setValue(value);
    }

    /**
     * Indicate that this metric is a custom performance counter and should be sent to the performance counters table.
     * This sets 'CustomPerfCounter'='true' key/value pair in this metric's properties.
     */
    public void markAsCustomPerfCounter(){
        data.getProperties().put("CustomPerfCounter", "true");
    }

    /**
     * Gets the name of the metric.
     * @return The name of the metric.
     */
    public String getName() {
        return metric.getName();
    }

    /**
     * Sets the name of the metric. Length 1-150 characters.
     * @param name The name of the metric.
     * @throws IllegalArgumentException if the name is null or empty.
     */
    public void setName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("The metric name cannot be null or empty");
        }

        metric.setName(name);
    }

    /**
     * Gets The value of the metric. Represents the sum of data points if this metric is an Aggregation
     * @return The value of the metric.
     */
    public double getValue() {
        return metric.getValue();
    }

    /**
     * Sets The value of the metric.
     * @param value The value of the metric.
     */
    public void setValue(double value) {
        metric.setValue(value);
    }

    /**
     * Gets the number of samples for this metric.
     * @return Number of samples.
     */
    public Integer getCount() {
        return metric.getCount();
    }

    /**
     * Sets the number of samples for this metric.
     * @param count Number of samples greater than or equal to 1
     */
    public void setCount(Integer count) {
        metric.setCount(count); updateKind();
    }

    /**
     * Gets the min value of this metric across samples.
     * @return The min value.
     */
    public Double getMin() {
        return metric.getMin();
    }

    /**
     * Sets the min value of this metric across samples.
     * @param value The min value.
     */
    public void setMin(Double value) {
        metric.setMin(value); updateKind();
    }

    /**
     * Gets the max value of this metric across samples.
     * @return The max value.
     */
    public Double getMax() {
        return metric.getMax();
    }

    /**
     * Sets the max value of this metric across samples.
     * @param value The max value.
     */
    public void setMax(Double value) {
        metric.setMax(value); updateKind();
    }

    /**
     * Gets the standard deviation of this metric across samples.
     * @return The max value.
     */
    public Double getStandardDeviation() {
        return metric.getStdDev();
    }

    /**
     * Sets the standard deviation of this metric across samples.
     * @param value The max value.
     */
    public void setStandardDeviation(Double value) {
        metric.setStdDev(value); updateKind();
    }

    @Deprecated
    @Override
    protected void additionalSanitize() {
        metric.setName(Sanitizer.sanitizeName(metric.getName()));
    }

    @Override
    protected MetricData getData() {
        return data;
    }

    private void updateKind() {
        // if any stats are set, assume it's an aggregation.
        boolean isAggregation =
            (metric.getCount() != null) ||
            (metric.getMin() != null) ||
            (metric.getMax() != null) ||
            (metric.getStdDev() != null);

        metric.setKind(isAggregation ? DataPointType.Aggregation : DataPointType.Measurement);
    }

    @Override
    public String getEnvelopName() {
        return ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return BASE_TYPE;
    }

    public DataPointType getKind() {
        return metric.getKind();
    }

}
