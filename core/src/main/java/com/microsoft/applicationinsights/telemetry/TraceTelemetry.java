/*
 * AppInsights-Java
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

import com.google.common.base.Strings;
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
        this("");
    }

    public TraceTelemetry(String message) {
        this(message, null);
    }

    /**
     * Initializes a new instance of the class with the specified parameter 'message'.
     * @param message The message.
     */
    public TraceTelemetry(String message, SeverityLevel severityLevel) {
        super();

        data = new MessageData();
        initialize(data.getProperties());

        setMessage(message);
        setSeverityLevel(severityLevel);
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

    public void setSeverityLevel(SeverityLevel severityLevel) {
        data.setSeverityLevel(severityLevel);
    }

    public SeverityLevel getSeverityLevel() {
        return data.getSeverityLevel();
    }
}
