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

package com.microsoft.applicationinsights.internal.schemav2;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;

/**
 * Data contract class MessageData.
 */
public class MessageData extends Domain implements JsonSerializable {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String MESSAGE_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Message";

    /**
     * Base Type for this telemetry.
     */
    public static final String MESSAGE_BASE_TYPE = "Microsoft.ApplicationInsights.MessageData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property Message.
     */
    private String message;

    /**
     * Backing field for property SeverityLevel.
     */
    private SeverityLevel severityLevel = null;

    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;

    /**
     * Initializes a new instance of the class.
     */
    public MessageData()
    {
        this.InitializeFields();
    }

    public int getVer() {
        return this.ver;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String value) {
        this.message = value;
    }

    public SeverityLevel getSeverityLevel() {
        return this.severityLevel;
    }

    public void setSeverityLevel(SeverityLevel value) {
        this.severityLevel = value;
    }

    public ConcurrentMap<String, String> getProperties() {
        if (this.properties == null) {
            this.properties = new ConcurrentHashMap<String, String>();
        }
        return this.properties;
    }

    public void setProperties(ConcurrentMap<String, String> value) {
        this.properties = value;
    }


    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("ver", ver);
        writer.write("message", message);

        if (severityLevel != null) {
            writer.write("severityLevel", severityLevel.toString());
        }

        writer.write("properties", properties);
    }

    @Override
    public String getEnvelopName() {
        return MESSAGE_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return MESSAGE_BASE_TYPE;
    }

    protected void InitializeFields() {
    }
}
