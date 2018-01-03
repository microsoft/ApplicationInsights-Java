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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.schemav2.PerformanceCounterData;

/**
 * The class that represents information about performance counters.
 */
public final class PerformanceCounterTelemetry extends BaseTelemetry<PerformanceCounterData> {
    private final PerformanceCounterData data;

    /**
     * Envelope Name for this telemetry.
     */
    private static final String ENVELOPE_NAME = "PerformanceCounter";


    /**
     * Base Type for this telemetry.
     */
    private static final String BASE_TYPE = "PerformanceCounterData";


    public PerformanceCounterTelemetry() {
        data = new PerformanceCounterData();
        initialize(data.getProperties());
    }

    /**
     * Initializes the instance with all the needed data.
     * @param categoryName Must be non null, non empty value.
     * @param counterName Must be non null, non empty value.
     * @param instanceName The instance name.
     * @param value The value of the performance counter.
     */
    public PerformanceCounterTelemetry(String categoryName, String counterName, String instanceName, double value) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(categoryName), "categoryName must be non null, non empty value");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(counterName), "counterName must be non null, non empty value");

        data = new PerformanceCounterData();
        initialize(data.getProperties());

        data.setCategoryName(categoryName);
        data.setCounterName(counterName);
        data.setInstanceName(instanceName);
        data.setValue(value);
    }

    /**
     * Sets the category name of the performance counter.
     * @param categoryName The category name.
     */
    public void setCategoryName(String categoryName) {
        data.setCategoryName(categoryName);
    }

    /**
     * Gets the category name of the performance counter.
     * @return The category name.
     */
    public String getCategoryName() {
        return data.getCategoryName();
    }

    /**
     * Sets the counter name of the performance counter.
     * @param counterName The counter name.
     */
    public void setCounterName(String counterName) {
        data.setCounterName(counterName);
    }

    /**
     * Gets the counter name of the performance counter.
     * @return The counter name.
     */
    public String getCounterName() {
        return data.getCounterName();
    }

    /**
     * Sets the instance name of the performance counter.
     * @param instanceName The instance name.
     */
    public void setInstanceName(String instanceName) {
        data.setInstanceName(instanceName);
    }

    /**
     * Gets the instance name of the performance counter.
     * @return The instance name.
     */
    public String getInstanceName() {
        return data.getInstanceName();
    }

    /**
     * Sets the value of the performance counter.
     * @param value The value.
     */
    public void setValue(double value) {
        data.setValue(value);
    }

    /**
     * Gets the value of the performance counter.
     * @return The value.
     */
    public double getValue() {
        return data.getValue();
    }

    @Override
    @Deprecated
    protected void additionalSanitize() {

    }

    @Override
    protected PerformanceCounterData getData() {
        return data;
    }

    @Override
    public String getEnvelopName() {
        return ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return BASE_TYPE;
    }

}
