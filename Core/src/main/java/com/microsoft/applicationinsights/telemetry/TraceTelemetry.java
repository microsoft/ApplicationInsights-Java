package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.util.Sanitizer;

/**
 * Telemetry type used for log messages.
 */
public final class TraceTelemetry extends BaseTelemetry<MessageData> {
    private final MessageData data;

    /**
     * Default Ctor
     */
    public TraceTelemetry() {
        super();
        data = new MessageData();
        initialize(data.getProperties());
    }

    /**
     * Initializes a new instance of the class with the specified {@param message}.
     * @param message The message.
     */
    public TraceTelemetry(String message) {
        this();
        this.setMessage(message);
    }

    /**
     * Gets the message text. For example, the text that would normally be written to a log file line.
     * @return The message.
     */
    public String getMessage() {
        return data.getMessage();
    }

    /**
     * Sets the message text. For example, the text that would normally be written to a log file line.
     * @param message The message.
     */
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
