package com.microsoft.applicationinsights.datacontracts;

import com.microsoft.applicationinsights.implementation.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.util.LocalStringsUtils;

import com.google.common.base.Strings;

/**
 * Telemetry used to track events.
 */
public class RemoteDependencyTelemetry extends BaseTelemetry<RemoteDependencyData> {
    private final RemoteDependencyData data;

    public RemoteDependencyTelemetry() {
        super();
        this.data = new RemoteDependencyData();
        initialize(this.data.getProperties());
    }

    public RemoteDependencyTelemetry(String name) {
        this();
        this.setName(name);
    }

    public String getName() {
        return this.data.getName();
    }

    public void setName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("The event name cannot be null or empty");
        }

        this.data.setName(name);
    }

    public double getValue() {
        return this.data.getValue();
    }

    public void setValue(double value) {
        this.data.setValue(value);
    }

    @Override
    protected void additionalSanitize() {
        this.data.setName(LocalStringsUtils.sanitize(this.data.getName(), 1024));
    }

    @Override
    protected RemoteDependencyData getData() {
        return data;
    }
}
