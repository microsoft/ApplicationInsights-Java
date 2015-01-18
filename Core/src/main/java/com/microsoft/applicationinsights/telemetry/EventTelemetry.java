package com.microsoft.applicationinsights.telemetry;

import java.util.Map;

import com.microsoft.applicationinsights.internal.schemav2.EventData;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.util.Sanitizer;

/**
 * Telemetry type used to track events.
 */
public final class EventTelemetry extends BaseTelemetry<EventData> {
    private final EventData data;

    /**
     * Default initialization for a new instance.
     */
    public EventTelemetry() {
        super();
        data = new EventData();
        initialize(data.getProperties());
    }

    /**
     * Initializes a new instance.
     */
    public EventTelemetry(String name) {
        this();
        this.setName(name);
    }

    /**
     * Gets a map of application-defined event metrics.
     * @return The map of metrics
     */
    public Map<String, Double> getMetrics() {
        return data.getMeasurements();
    }

    /**
     * Gets the name of the event.
     * @return The name
     */
    public String getName() {
        return data.getName();
    }

    /**
     * Sets the name of the event.
     * @param name Name of the event
     */
    public void setName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("The event name cannot be null or empty");
        }

        data.setName(name);
    }

    /**
     * Sanitize additional stuff besides the common
     */
    @Override
    protected void additionalSanitize() {
        data.setName(Sanitizer.sanitizeName(data.getName()));
        Sanitizer.sanitizeMeasurements(this.getMetrics());
    }

    /**
     * Fetches the data structure the instance works with
     * @return
     */
    @Override
    protected EventData getData() {
        return data;
    }
}
