package com.microsoft.applicationinsights.telemetry;

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

    public double getValue() {
        return data.getValue();
    }

    public void setValue(double value) {
        data.setValue(value);
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
