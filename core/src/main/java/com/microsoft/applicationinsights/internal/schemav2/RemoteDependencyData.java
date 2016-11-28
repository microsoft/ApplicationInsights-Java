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

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

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
    public static final String REMOTE_BASE_TYPE = "RemoteDependencyData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property Name.
     */
    private String name;

    /**
     * Backing field for property commandName.
     */
    private String commandName;

    private int resultCode;

    /**
     * Backing field for property Kind.
     */
    private DataPointType kind = DataPointType.Measurement;

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
     * Backing field for property DependencyKind.
     */
    private DependencyKind dependencyKind = DependencyKind.Other;

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
     * Backing field for property Duration.
     */
    private Duration duration;

    private String type;

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

    public String getCommandName() { return this.commandName; }

    public void setCommandName(String commandName) { this.commandName = commandName; }

    public DataPointType getKind() {
        return this.kind;
    }

    public void setKind(DataPointType value) {
        this.kind = value;
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

    public Duration getDuration() {
        return this.duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
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
        writer.write("resultCode", resultCode);
        writer.write("commandName", commandName);
        writer.write("kind", kind.getValue());
        writer.write("value", duration != null ? duration.getTotalMilliseconds() : 0);
        writer.write("count", count);
        writer.write("min", min);
        writer.write("max", max);
        writer.write("stdDev", stdDev);
        writer.write("type", type);
//        writer.write("dependencyKind", dependencyKind.getValue());
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

    protected void InitializeFields() {
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }
}
