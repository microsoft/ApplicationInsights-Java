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

import com.microsoft.applicationinsights.telemetry.DependencyKind;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Data contract class RemoteDependencyData.
 */
public class RemoteDependencyData extends Domain {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String REMOTE_ENVELOPE_NAME = "Microsoft.ApplicationInsights.RemoteDependency";

    /**
     * Base Type for this telemetry.
     */
    public static final String REMOTE_BASE_TYPE = "Microsoft.ApplicationInsights.RemoteDependencyData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

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
    private double value;

    /**
     * Backing field for property Count.
     */
    private Integer count;

    /**
     * Backing field for property DependencyKind.
     */
    private DependencyKind dependencyKind = DependencyKind.Undefined;

    /**
     * Backing field for property Success.
     */
    private Boolean success = true;

    /**
     * Backing field for property Async.
     */
    private Boolean async;

    /**
     * Backing field for property DependencySource.
     */
    private DependencySourceType dependencySource = DependencySourceType.Undefined;

    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;

    /**
     * Initializes a new instance of the class.
     */
    public RemoteDependencyData()
    {
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

    public DependencyKind getDependencyKind() {
        return this.dependencyKind;
    }

    public void setDependencyKind(DependencyKind value) {
        this.dependencyKind = value;
    }

    public Boolean getSuccess() {
        return this.success;
    }

    public void setSuccess(Boolean value) {
        this.success = value;
    }

    public Boolean getAsync() {
        return this.async;
    }

    public void setAsync(Boolean value) {
        this.async = value;
    }

    public DependencySourceType getDependencySource() {
        return this.dependencySource;
    }

    public void setDependencySource(DependencySourceType value) {
        this.dependencySource = value;
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

    /**
     * Serializes the beginning of this object to the passed in writer.
     * @param writer The writer to serialize this object to.
     * @throws IOException Might be thrown during serialization.
     */
    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("ver", ver);
        writer.write("name", name);

        writer.write("kind", kind.getValue());

        writer.write("value", value);
        writer.write("count", count);

        writer.write("dependencyKind", dependencyKind.getValue());
        writer.write("success", success);
        writer.write("async", async);
        writer.write("dependencySource", dependencySource.getValue());
        writer.write("properties", properties);
    }

    public String getEnvelopName() {
        return REMOTE_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return REMOTE_BASE_TYPE;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        if (!(other instanceof RemoteDependencyData)) {
            return false;
        }

        RemoteDependencyData that = (RemoteDependencyData)other;
        return this.ver == that.getVer() &&
               this.success == null ? that.getSuccess() == null : this.success.equals(that.getSuccess()) &&
               this.async == null ? that.getAsync() == null : this.async.equals(that.getAsync()) &&
               this.value == that.getValue() &&
               this.count == that.getCount() &&
               this.name == null ? that.getName() == null : this.name.equals(that.getName()) &&
               this.dependencyKind == null ? that.getDependencyKind() == null : this.dependencyKind.equals(that.getDependencyKind()) &&
               this.dependencySource == null ? that.getDependencySource() == null : this.dependencySource.equals(that.getDependencySource()) &&
               this.getProperties().equals(that.getProperties());
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return hash * 89 + new HashCodeBuilder(17, 31).
                append(ver).
                append(success).
                append(async).
                append(value).
                append(count).
                append(name).
                append(dependencyKind).
                append(dependencySource).
                append(getProperties()).
                toHashCode();
    }

    protected void InitializeFields() {
    }
}
