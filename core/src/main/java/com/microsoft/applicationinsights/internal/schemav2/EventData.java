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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.Preconditions;

/**
 * Data contract class EventData.
 */
public class EventData extends Domain {
    /**
     * Envelope Name for this telemetry.
     */
    private static final String EVENT_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Event";

    /**
     * Base Type for this telemetry.
     */
    private static final String EVENT_BASE_TYPE = "Microsoft.ApplicationInsights.EventData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property Name.
     */
    private String name;

    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;

    /**
     * Backing field for property Measurements.
     */
    private ConcurrentMap<String, Double> measurements;

    /**
     * Initializes a new instance of the class.
     */
    public EventData() {
        this.InitializeFields();
    }

    public int getVer() {
        return this.ver;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
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

    public ConcurrentMap<String, Double> getMeasurements() {
        if (this.measurements == null) {
            this.measurements = new ConcurrentHashMap<String, Double>();
        }
        return this.measurements;
    }

    public void setMeasurements(ConcurrentMap<String, Double> value) {
        this.measurements = value;
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        writer.write("ver", ver);
        writer.write("name", name);
        writer.write("properties", properties);
        writer.write("measurements", measurements);
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        if (!(other instanceof PageViewData)) {
            return false;
        }

        EventData that = (EventData)other;
        return this.ver == that.getVer() &&
               this.name == null ? that.getName() == null : this.name.equals(that.getName()) &&
               this.getMeasurements().equals(that.getMeasurements()) &&
               this.getProperties().equals(that.getProperties());
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return hash * 89 + new HashCodeBuilder(17, 31).
                append(ver).
                append(name).
                append(getMeasurements()).
                append(getProperties()).
                toHashCode();
    }

    @Override
    public String getEnvelopName() {
        return EVENT_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return EVENT_BASE_TYPE;
    }

    protected void InitializeFields() {
    }
}
