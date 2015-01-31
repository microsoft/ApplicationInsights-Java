/*
 * AppInsights-Java
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Data contract class MetricData.
 */
public final class MetricData extends Domain implements JsonSerializable {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String METRIC_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Metric";

    /**
     * Base Type for this telemetry.
     */
    public static final String METRIC_BASE_TYPE = "Microsoft.ApplicationInsights.MetricData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property Metrics.
     */
    private List<DataPoint> metrics;

    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;

    /**
     * Initializes a new instance of the class.
     */
    public MetricData()
    {
        this.InitializeFields();
    }

    public int getVer() {
        return this.ver;
    }

    public List<DataPoint> getMetrics() {
        if (this.metrics == null) {
            this.metrics = new ArrayList<DataPoint>();
        }
        return this.metrics;
    }

    public void setMetrics(List<DataPoint> value) {
        this.metrics = value;
    }

    public ConcurrentMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new ConcurrentHashMap<String, String>();
        }
        return this.properties;
    }

    public void setProperties(ConcurrentMap<String, String> value) {
        this.properties = value;
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("ver", ver);
        writer.write("metrics", metrics);
        writer.write("properties", properties);
    }

    @Override
    public String getEnvelopName() {
        return METRIC_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return METRIC_BASE_TYPE;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        if (!(other instanceof MetricData)) {
            return false;
        }

        MetricData that = (MetricData)other;
        return this.ver == that.getVer() &&
               this.getMetrics().equals(that.getMetrics()) &&
               this.getProperties().equals(that.getProperties());
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return hash * 89 + new HashCodeBuilder(17, 31).
                append(ver).
                append(getMetrics()).
                append(getProperties()).
                toHashCode();
    }

    protected void InitializeFields() {
    }
}
