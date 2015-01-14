package com.microsoft.applicationinsights.telemetry;

import java.util.Map;

import com.microsoft.applicationinsights.internal.schemav2.EventData;

import com.google.common.base.Strings;

/**
 * Telemetry used to track events.
 */
public final class EventTelemetry extends BaseTelemetry<EventData> {
    private final EventData data;

    public EventTelemetry() {
        super();
        data = new EventData();
        initialize(data.getProperties());
    }

    public EventTelemetry(String name) {
        this();
        this.setName(name);
    }

    public Map<String,Double> getMetrics() {
        return data.getMeasurements();
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

    @Override
    protected void additionalSanitize() {
        data.setName(Sanitizer.sanitizeName(data.getName()));
        Sanitizer.sanitizeMeasurements(this.getMetrics());
    }

    @Override
    protected EventData getData() {
        return data;
    }
}
