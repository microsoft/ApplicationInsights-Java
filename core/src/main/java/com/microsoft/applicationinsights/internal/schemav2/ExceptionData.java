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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Data contract class ExceptionData.
 */
public final class ExceptionData extends Domain implements JsonSerializable {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String EXCEPTION_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Exception";

    /**
     * Base Type for this telemetry.
     */
    public static final String EXCEPTION_BASE_TYPE = "Microsoft.ApplicationInsights.ExceptionData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property HandledAt.
     */
    private String handledAt;

    /**
     * Backing field for property Exceptions.
     */
    private List<ExceptionDetails> exceptions;

    /**
     * Backing field for property SeverityLevel.
     */
    private SeverityLevel severityLevel = null;

    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;

    /**
     * Backing field for property Measurements.
     */
    private ConcurrentMap<String, Double> measurements;

    /**
     * Initializes a new instance of the \class.
     */
    public ExceptionData()
    {
        this.InitializeFields();
    }

    public int getVer() {
        return this.ver;
    }

    public String getHandledAt() {
        return this.handledAt;
    }

    public void setHandledAt(String value) {
        this.handledAt = value;
    }

    public List<ExceptionDetails> getExceptions() {
        if (this.exceptions == null) {
            this.exceptions = new ArrayList<ExceptionDetails>();
        }
        return this.exceptions;
    }

    public void setExceptions(List<ExceptionDetails> value) {
        this.exceptions = value;
    }

    public SeverityLevel getSeverityLevel() {
        return this.severityLevel;
    }

    public void setSeverityLevel(SeverityLevel value) {
        this.severityLevel = value;
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

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be thrown during serialization.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer must be a non-null value");

        super.serializeContent(writer);

        writer.write("ver", ver);
        writer.write("handledAt", handledAt);
        writer.write("exceptions", exceptions);

        if (severityLevel != null) {
            writer.write("severityLevel", severityLevel.toString());
        }

        writer.write("properties", properties);
        writer.write("measurements", measurements);
    }

    @Override
    public String getEnvelopName() {
        return EXCEPTION_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return EXCEPTION_BASE_TYPE;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        if (!(other instanceof ExceptionData)) {
            return false;
        }

        boolean rv = super.equals( other );

        ExceptionData that = (ExceptionData)other;
        return this.ver == that.getVer() &&
               this.handledAt == null ? that.getHandledAt() == null : this.handledAt.equals(that.getHandledAt()) &&
               this.severityLevel == null ? that.getSeverityLevel() == null : this.severityLevel.equals(that.getSeverityLevel()) &&
               this.getMeasurements().equals(that.getMeasurements()) &&
               this.getProperties().equals(that.getProperties());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(ver).
                append(handledAt).
                append(severityLevel).
                append(getMeasurements()).
                append(getProperties()).
                toHashCode();
    }

    protected void InitializeFields() {
    }
}
