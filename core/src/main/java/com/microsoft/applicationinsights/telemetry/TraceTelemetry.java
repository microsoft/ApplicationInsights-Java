/*
 * ApplicationInsights-Java
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

import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.util.Sanitizer;

/**
 * Telemetry type used for log messages.
 */
public final class TraceTelemetry extends BaseSampleSourceTelemetry<MessageData> {
    private Double samplingPercentage;
    private final MessageData data;

    /**
     * Envelope Name for this telemetry.
     */
    private static final String ENVELOPE_NAME = "Microsoft.ApplicationInsights.Message";


    /**
     * Base Type for this telemetry.
     */
    private static final String BASE_TYPE = "MessageData";


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
     * @param message The message. Max length 10000.
     * @param severityLevel The severity level.
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
        data.setSeverityLevel(severityLevel == null ? null : com.microsoft.applicationinsights.internal.schemav2.SeverityLevel.values()[severityLevel.getValue()]);
    }

    public SeverityLevel getSeverityLevel() {
        return data.getSeverityLevel() == null ? null : SeverityLevel.values()[data.getSeverityLevel().getValue()];
    }

    @Override
    public Double getSamplingPercentage() {
        return samplingPercentage;
    }

    @Override
    public void setSamplingPercentage(Double samplingPercentage) {
        this.samplingPercentage = samplingPercentage;
    }

    @Override
    public String getEnvelopName() {
        return ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return BASE_TYPE;
    }

}
