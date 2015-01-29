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

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Data contract class DataPoint.
 */
public final class DataPoint implements JsonSerializable, SendableData {
    /**
     * Backing field for property Name.
     */
    private String name;

    /**
     * Backing field for property Kind.
     */
    private DataPointType kind = DataPointType.Measurement;

    /**
     * Backing field for property Value.
     */
    private Double value;

    /**
     * Backing field for property Count.
     */
    private Integer count;

    /**
     * Backing field for property Min.
     */
    private Double min;

    /**
     * Backing field for property Max.
     */
    private Double max;

    /**
     * Backing field for property StdDev.
     */
    private Double stdDev;

    /**
     * Initializes a new instance of the class.
     */
    public DataPoint()
    {
        this.InitializeFields();
    }

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

    public Double getValue() {
        return this.value;
    }

    public void setValue(Double value) {
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

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     */
    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {
        Preconditions.checkNotNull(writer, "writer should be a nno-null value");

        serializeContent(writer);
    }

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be throw during serialization.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        writer.write("name", name);

        writer.write("kind", kind.toString());
        writer.write("value", value);
        writer.write("count", count);
        writer.write("min", min);
        writer.write("max", max);
        writer.write("stdDev", stdDev);
    }

    /**
     * Optionally initializes fields for the current context.
     */
    protected void InitializeFields() {
    }

    @Override
    public String getEnvelopName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBaseTypeName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof DataPoint)) {
            return false;
        }

        DataPoint that = (DataPoint)other;
        return this.name == null ? that.getName() == null : this.name.equals(that.getName()) &&
               this.kind == null ? that.getKind() == null : this.kind.equals(that.getKind()) &&
               this.value == null ? that.getValue() == null : this.value.equals(that.getValue()) &&
               this.count == null ? that.getCount() == null : this.count.equals(that.getCount()) &&
               this.min == null ? that.getMin() == null : this.min.equals(that.getMin()) &&
               this.max == null ? that.getMax() == null : this.max.equals(that.getMax()) &&
               this.stdDev == null ? that.getStdDev() == null : this.stdDev.equals(that.getStdDev());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(name).
                append(kind).
                append(value).
                append(count).
                append(min).
                append(max).
                append(stdDev).
                toHashCode();
    }
}
