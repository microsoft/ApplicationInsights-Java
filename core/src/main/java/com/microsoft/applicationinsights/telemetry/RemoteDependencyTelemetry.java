package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.DependencyKind;
import com.microsoft.applicationinsights.internal.schemav2.DependencySourceType;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.util.Sanitizer;

/**
 * The class that represents information about collected RDD.
 */
public final class RemoteDependencyTelemetry extends BaseTelemetry<RemoteDependencyData> {
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
     * @param name The resource name.
     */
    public RemoteDependencyTelemetry(String name) {
        this();
        setName(name);
    }

    /**
     * Gets tne name resource name.
     * @return The resource name.
     */
    public String getName() {
        return data.getName();
    }

    /**
     * Sets the resource name.
     * @param name The resource name.
     */
    public void setName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("The event name cannot be null or empty");
        }

        data.setName(name);
    }

    /**
     * Gets the Value property.
     * @return Value property.
     */
    public double getValue() {
        return data.getValue();
    }

    /**
     * Sets the Value property.
     * @param value Value property.
     */
    public void setValue(double value) {
        data.setValue(value);
    }

    /**
     * Gets the Count property.
     * @return Count property.
     */
    public Integer getCount() {
        return data.getCount();
    }

    /**
     * Sets the Count property.
     * @param value Count property.
     */
    public void setCount(Integer value) {
        data.setCount(value);
    }

    /**
     * Gets the Min property.
     * @return Min property.
     */
    public Double getMin() {
        return data.getMin();
    }

    /**
     * Sets the Min property.
     * @param value Min property.
     */
    public void setMin(Double value) {
        data.setMin(value);
    }

    /**
     * Gets the Max property.
     * @return Max property.
     */
    public Double getMax() {
        return data.getMax();
    }

    /**
     * Sets the Max property.
     * @param value Max property.
     */
    public void setMax(Double value) {
        data.setMax(value);
    }

    /**
     * Gets the Standard Deviation property.
     * @return Standard Deviation property.
     */
    public Double getStdDev() {
        return data.getStdDev();
    }

    /**
     * Sets the StdDev property.
     * @param value Standard Deviation property.
     */
    public void setStdDev(Double value) {
        data.setStdDev(value);
    }

    /**
     * Gets the Dependency Kind property.
     * @return Dependency Kind property.
     */
    public DependencyKind getDependencyKind() {
        return data.getDependencyKind();
    }

    /**
     * Sets the Dependency Kind property.
     * @param value Dependency Kind property.
     */
    public void setDependencyKind(DependencyKind value) {
        data.setDependencyKind(value);
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
    public Boolean getAsync() {
        return data.getAsync();
    }

    /**
     * Sets the Async property.
     * @param value True if async.
     */
    public void setAsync(Boolean value) {
        data.setAsync(value);
    }

    /**
     * Gets the Dependency Source property.
     * @return Dependency Source property.
     */
    public DependencySourceType getDependencySource() {
        return data.getDependencySource();
    }

    /**
     * Sets the Dependency Source property.
     * @param value Dependency Source property.
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
