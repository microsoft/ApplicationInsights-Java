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
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.JsonTelemetryDataSerializer;

/**
 * Data contract class RequestData.
 */
public class RequestData extends Domain {
    /**
     * Envelope Name for this telemetry.
     */
    public static final String REQUEST_ENVELOPE_NAME = "Microsoft.ApplicationInsights.Request";

    /**
     * Base Type for this telemetry.
     */
    public static final String REQUEST_BASE_TYPE = "Microsoft.ApplicationInsights.RequestData";

    /**
     * Backing field for property Ver.
     */
    private int ver = 2;

    /**
     * Backing field for property Id.
     */
    private String id;

    /**
     * Backing field for property Name.
     */
    private String name;

    /**
     * Backing field for property StartTime.
     */
    private Date startTime;

    /**
     * Backing field for property Duration.
     */
    private Duration duration;

    /**
     * Backing field for property ResponseCode.
     */
    private String responseCode;

    /**
     * Backing field for property Success.
     */
    private boolean success;

    /**
     * Backing field for property HttpMethod.
     */
    private String httpMethod;

    /**
     * Backing field for property Url.
     */
    private String url;

    /**
     * Backing field for property Properties.
     */
    private ConcurrentMap<String, String> properties;

    /**
     * Backing field for property Measurements.
     */
    private ConcurrentMap<String, Double> measurements;

    /**
     * Initializes a new instance of the class.
     */
    public RequestData()
    {
        this.InitializeFields();
    }

    public int getVer() {
        return this.ver;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public Date getStartTime() {
        return this.startTime;
    }

    public void setStartTime(Date value) {
        this.startTime = value;
    }

    public Duration getDuration() {
        return this.duration;
    }

    public void setDuration(Duration value) {
        this.duration = value;
    }

    public String getResponseCode() {
        return this.responseCode;
    }

    public void setResponseCode(String value) {
        this.responseCode = value;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean value) {
        this.success = value;
    }

    public String getHttpMethod() {
        return this.httpMethod;
    }

    public void setHttpMethod(String value) {
        this.httpMethod = value;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String value) {
        this.url = value;
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

    public ConcurrentMap<String, Double> getMeasurements() {
        if (this.measurements == null) {
            this.measurements = new ConcurrentHashMap<String, Double>();
        }
        return this.measurements;
    }

    public void setMeasurements(ConcurrentMap<String, Double> value) {
        this.measurements = value;
    }

    @Override
    public String getEnvelopName() {
        return REQUEST_ENVELOPE_NAME;
    }

    @Override
    public String getBaseTypeName() {
        return REQUEST_BASE_TYPE;
    }

    protected void serializeContent(JsonTelemetryDataSerializer writer) throws IOException {
        super.serializeContent(writer);

        writer.write("ver", ver);
        writer.write("id", id);
        writer.write("name", name);
        writer.write("startTime", startTime);
        writer.write("duration", duration != null ? duration.toString() : "");
        writer.write("responseCode", responseCode);
        writer.write("success", success);
        writer.write("httpMethod", httpMethod);
        writer.write("url", url);
        writer.write("properties", properties);
        writer.write("measurements", measurements);
    }

    protected void InitializeFields() {
    }
}
