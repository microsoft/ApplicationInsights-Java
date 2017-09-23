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

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @deprecated
 * Created by gupele on 3/1/2015.
 */
@Deprecated
public final class PerformanceCounterData extends Domain {
    /**
     * Envelope Name for this telemetry.
     */
    private static final String PERFORMANCE_COUNTER_ENVELOPE_NAME = "Microsoft.ApplicationInsights.PerformanceCounter";

    /**
     * Base Type for this telemetry.
     */
    private static final String PERFORMANCE_COUNTER_BASE_TYPE = "PerformanceCounterData";

    private final int ver = 2;
    private String categoryName;
    private String counterName;
    private String instanceName;
    private double value;
    private ConcurrentMap<String, String> properties;

    public int getVer() {
        return ver;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryName() {
        return categoryName;
    }
    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public String getCounterName() {
        return counterName;
    }
    public void setValue(double value) {
        this.value = value;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public double getValue() {
        return value;
    }

    public ConcurrentMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new ConcurrentHashMap<String, String>();
        }
        return this.properties;
    }

    @Override
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        writer.write("ver", ver);
        writer.write("categoryName", categoryName, 1000);
        writer.write("counterName", counterName, 1000);
        writer.write("instanceName", instanceName, 1000);
        writer.write("value", value);
        writer.write("properties", properties);
    }
}