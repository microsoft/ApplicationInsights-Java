package com.microsoft.applicationinsights.telemetry;

import java.util.Map;

import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.MapUtil;

import com.google.common.base.Strings;

/**
 * Telemetry used to track events.
 */
public final class EventTelemetry extends BaseTelemetry<EventData> {
    private final EventData data;

    public EventTelemetry() {
        super();
        this.data = new EventData();
        initialize(this.data.getProperties());
    }

    public EventTelemetry(String name) {
        this();
        this.setName(name);
    }

    public Map<String,Double> getMetrics() {
        return this.data.getMeasurements();
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

    @Override
    protected void additionalSanitize() {
        this.data.setName(LocalStringsUtils.sanitize(this.data.getName(), 1024));
        MapUtil.sanitizeMeasurements(this.getMetrics());
    }

    @Override
    protected EventData getData() {
        return data;
    }
}
