package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.MessageData;

/**
 * Telemetry used to track events.
 */
public final class TraceTelemetry extends BaseTelemetry<MessageData> {
    private final MessageData data;

    public TraceTelemetry() {
        super();
        data = new MessageData();
        initialize(data.getProperties());
    }

    public TraceTelemetry(String message) {
        this();
        this.setMessage(message);
    }

    public void setMessage(String message) {
        data.setMessage(message);
    }

    @Override
    protected void additionalSanitize() {
        data.setMessage(Sanitizer.sanitizeMessage(data.getMessage()));
    }

    @Override
    protected MessageData getData() {
        return data;
    }
}
