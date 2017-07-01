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

import com.microsoft.applicationinsights.internal.schemav2.DependencyKind;
import com.microsoft.applicationinsights.internal.schemav2.DependencySourceType;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.Sanitizer;
import org.apache.http.annotation.Obsolete;

/**
 * Telemetry sent to Azure Application Insights about dependencies - that is, calls from
 * your application to external services such as databases or REST APIs.
 */
public final class RemoteDependencyTelemetry extends BaseSampleSourceTelemetry<RemoteDependencyData> {
    private Double samplingPercentage;
    private final RemoteDependencyData data;

    /**
     * Default Ctor
     */
    public RemoteDependencyTelemetry() {
        super();
        data = new RemoteDependencyData();
        initialize(this.data.getProperties());
    }

    /**
     * Initializes an instance with a 'name'
     * @param name The dependency name.
     */
    public RemoteDependencyTelemetry(String name) {
        this();
        setName(name);
    }

    /**
     * Initializes an instnace with the given parameters.
     * @param dependencyName The dependency name.
     * @param commandName The command name or call details.
     * @param duration How long it took to process the call.
     * @param success Whether the remote call successful or not.
     */
    public RemoteDependencyTelemetry(String dependencyName, String commandName, Duration duration, boolean success) {
        this(dependencyName);

        this.data.setData(commandName);
        this.data.setDuration(duration);
        this.data.setSuccess(success);
    }

    /**
     * Gets tne dependency name.
     * @return The dependency name.
     */
    public String getName() {
        return data.getName();
    }

    /**
     * Sets the dependency name.
     * @param name The dependency name.
     */
    public void setName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("The event name cannot be null or empty");
        }

        data.setName(name);
    }

    /**
     * Gets the command name.
     * @return The command name.
     */
    public String getCommandName() { return this.data.getData(); }

    /**
     * Sets the command name.
     * @param commandName The command name.
     */
    public void setCommandName(String commandName) { this.data.setData(commandName); }

    /**
     * Gets the Count property.
     * @return Count property.
     */
    @Obsolete
    public Integer getCount() {
        return null;
    }

    /**
     * Sets the Count property.
     * @param value Count property.
     */
    @Obsolete
    public void setCount(Integer value) {
        //do nothing as this property is no longer in use
    }

    /**
     * Gets the Min property.
     * @return Min property.
     */
    @Obsolete
    public Double getMin() {
        return null;
    }

    /**
     * Sets the Min property.
     * @param value Min property.
     */
    @Obsolete
    public void setMin(Double value) {

    }

    /**
     * Gets the Max property.
     * @return Max property.
     */
    @Obsolete
    public Double getMax() {
        return null;
    }

    /**
     * Sets the Max property.
     * @param value Max property.
     */
    @Obsolete
    public void setMax(Double value) {

    }

    /**
     * Gets the Standard Deviation property.
     * @return Standard Deviation property.
     */
    @Obsolete
    public Double getStdDev() {
        return null;
    }

    /**
     * Sets the StdDev property.
     * @param value Standard Deviation property.
     */
    @Obsolete
    public void setStdDev(Double value) {
    }

    /**
     * Gets the Dependency Kind property.
     * @return Dependency Kind property.
     */
    @Obsolete
    public DependencyKind getDependencyKind() {
        DependencyKind result = DependencyKind.Other;
        String type = data.getType();
        if (!LocalStringsUtils.isNullOrEmpty(type)) {
            try {
                result = Enum.valueOf(DependencyKind.class, type);
            } catch (Throwable t) {
            }
        }
        return result;
    }

    /**
     * Sets the Dependency Kind property.
     * @param value Dependency Kind property.
     */
    @Obsolete
    public void setDependencyKind(DependencyKind value) {
        data.setType(value.toString());
    }

    /**
     * Gets the Type property.
     * @return type property.
     */
    public String getType() {
        return data.getType();
    }

    /**
     * Sets the type property.
     * @param value Type property.
     */
    public void setType(String value) {
        data.setType(value);
    }

    public void setResultCode(String value) {
        data.setResultCode(value);
    }

    /**
     * Gets the Success property.
     * @return True if success.
     */
    public boolean getSuccess() {
        return data.getSuccess();
    }

    /**
     * Sets the Success property.
     * @param value True if success.
     */
    public void setSuccess(boolean value) {
        data.setSuccess(value);
    }

    /**
     * Gets the Async property.
     * @return True if async.
     */
    @Obsolete
    public Boolean getAsync() {
        return false;
    }

    /**
     * Sets the Async property.
     * @param value True if async.
     */
    @Obsolete
    public void setAsync(Boolean value) {

    }

    /**
     * Gets the Dependency Source property.
     * @return Dependency Source property.
     */
    @Obsolete
    public DependencySourceType getDependencySource() {
        return DependencySourceType.Undefined;
    }

    /**
     * Sets the Dependency Source property.
     * @param value Dependency Source property.
     */
    @Obsolete
    public void setDependencySource(DependencySourceType value) {
    }

    /**
     * Gets the duration.
     * @return The duration.
     */
    public Duration getDuration() {
        return this.data.getDuration();
    }

    /**
     * Sets the duration.
     * @param duration The duration.
     */
    public void setDuration(Duration duration) {
        this.data.setDuration(duration);
    }

    @Override
    public Double getSamplingPercentage() {
        return samplingPercentage;
    }

    @Override
    public void setSamplingPercentage(Double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
    }

    @Override
    protected void additionalSanitize() {
        data.setName(Sanitizer.sanitizeName(data.getName()));
    }

    @Override
    protected RemoteDependencyData getData() {
        return data;
    }
}
