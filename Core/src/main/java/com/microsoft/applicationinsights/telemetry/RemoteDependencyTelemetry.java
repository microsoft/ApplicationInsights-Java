package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.DependencyKind;
import com.microsoft.applicationinsights.internal.schemav2.DependencySourceType;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;

import com.google.common.base.Strings;

/**
 * Telemetry used to track events.
 */
public final class RemoteDependencyTelemetry extends BaseTelemetry<RemoteDependencyData> {
    private final RemoteDependencyData data;

    public RemoteDependencyTelemetry() {
        super();
        data = new RemoteDependencyData();
        initialize(this.data.getProperties());
    }

    public RemoteDependencyTelemetry(String name) {
        this();
        this.setName(name);
    }

    public String getName() {
        return data.getName();
    }

    public void setName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("The event name cannot be null or empty");
        }

        data.setName(name);
    }

    /**
     * Gets the Value property.
     */
    public double getValue() {
        return data.getValue();
    }

    /**
     * Sets the Value property.
     */
    public void setValue(double value) {
        data.setValue(value);
    }

    /**
     * Gets the Count property.
     */
    public Integer getCount() {
        return data.getCount();
    }

    /**
     * Sets the Count property.
     */
    public void setCount(Integer value) {
        data.setCount(value);
    }

    /**
     * Gets the Min property.
     */
    public Double getMin() {
        return data.getMin();
    }

    /**
     * Sets the Min property.
     */
    public void setMin(Double value) {
        data.setMin(value);
    }

    /**
     * Gets the Max property.
     */
    public Double getMax() {
        return data.getMax();
    }

    /**
     * Sets the Max property.
     */
    public void setMax(Double value) {
        data.setMin(value);
    }

    /**
     * Gets the StdDev property.
     */
    public Double getStdDev() {
        return data.getStdDev();
    }

    /**
     * Sets the StdDev property.
     */
    public void setStdDev(Double value) {
        data.setStdDev(value);
    }

    /**
     * Gets the DependencyKind property.
     */
    public DependencyKind getDependencyKind() {
        return data.getDependencyKind();
    }

    /**
     * Sets the DependencyKind property.
     */
    public void setDependencyKind(DependencyKind value) {
        data.setDependencyKind(value);
    }

    /**
     * Gets the Success property.
     */
    public boolean getSuccess() {
        return data.getSuccess();
    }

    /**
     * Sets the Success property.
     */
    public void setSuccess(boolean value) {
        data.setSuccess(value);
    }

    /**
     * Gets the Async property.
     */
    public Boolean getAsync() {
        return data.getAsync();
    }

    /**
     * Sets the Async property.
     */
    public void setAsync(Boolean value) {
        data.setAsync(value);
    }

    /**
     * Gets the DependencySource property.
     */
    public DependencySourceType getDependencySource() {
        return data.getDependencySource();
    }

    /**
     * Sets the DependencySource property.
     */
    public void setDependencySource(DependencySourceType value) {
        data.setDependencySource(value);
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
