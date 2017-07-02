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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.util.Sanitizer;
import org.apache.http.HttpStatus;
import org.apache.http.annotation.Obsolete;

/**
 * Encapsulates information about a web request handled by the application.
 *
 * You can send information about requests processed by your web application to Application Insights by
 * passing an instance of this class to the 'trackRequest' method of the {@link com.microsoft.applicationinsights.TelemetryClient}
 */
public final class RequestTelemetry extends BaseSampleSourceTelemetry<RequestData> {
    private Double samplingPercentage;
    private final RequestData data;
    private String httpMethod;

    /**
     * Envelope Name for this telemetry.
     */
    private static final String ENVELOPE_NAME = "Microsoft.ApplicationInsights.Request";


    /**
     * Base Type for this telemetry.
     */
    private static final String BASE_TYPE = "RequestData";


    /**
     * Initializes a new instance of the HttpRequestTelemetry class.
     */
    public RequestTelemetry() {
        this.data = new RequestData();
        initialize(this.data.getProperties());
        setId(LocalStringsUtils.generateRandomIntegerId());

        // Setting mandatory fields.
        setTimestamp(new Date());
        setResponseCode(Integer.toString(HttpStatus.SC_OK));
        setSuccess(true);
    }

    /**
     * Initializes a new instance of the HttpRequestTelemetry class with the given name,
     * time stamp, duration, HTTP response code and success property values.
     * @param name A user-friendly name for the request.
     * @param timestamp The time of the request.
     * @param duration The duration, in milliseconds, of the request processing.
     * @param responseCode The HTTP response code.
     * @param success 'true' if the request was a success, 'false' otherwise.
     */
    public RequestTelemetry(String name, Date timestamp, long duration, String responseCode, boolean success) {
        this(name, timestamp, new Duration(duration), responseCode, success);
    }

    /**
     * Initializes a new instance of the HttpRequestTelemetry class with the given name,
     * time stamp, duration, HTTP response code and success property values.
     * @param name A user-friendly name for the request.
     * @param timestamp The time of the request.
     * @param duration The duration, as an {@link com.microsoft.applicationinsights.telemetry.Duration} instance, of the request processing.
     * @param responseCode The HTTP response code.
     * @param success 'true' if the request was a success, 'false' otherwise.
     */
    public RequestTelemetry(String name, Date timestamp, Duration duration, String responseCode, boolean success) {
        this.data = new RequestData();
        initialize(this.data.getProperties());

        setId(LocalStringsUtils.generateRandomIntegerId());

        setTimestamp(timestamp);

        setName(name);
        setDuration(duration);
        setResponseCode(responseCode);
        setSuccess(success);
    }

    /**
     * Gets a map of application-defined request metrics.
     * @return The map of metrics
     */
    ConcurrentMap<String, Double> getMetrics() {
        return data.getMeasurements();
    }

    /**
     * Sets the StartTime. Uses the default behavior and sets the property on the 'data' start time
     * @param timestamp he timestamp as Date.
     */
    @Override
    public void setTimestamp(Date timestamp) {
        if (timestamp == null) {
            timestamp = new Date();
        }

        super.setTimestamp(timestamp);
    }

    /**
     * Gets or human-readable name of the requested page.
     * @return A human-readable name
     */
    public String getName() {
        return data.getName();
    }

    /**
     * Sets or human-readable name of the requested page.
     * @param name A human-readable name
     */
    public void setName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("The event name cannot be null or empty");
        }
        data.setName(name);
    }

    /**
     * Gets the unique identifier of the request.
     * @return Unique identifier
     */
    public String getId()
    {
        return data.getId();
    }

    /**
     * Sets the unique identifier of the request.
     * @param id Unique identifier
     */
    public void setId(String id) {
        data.setId(id);
    }

    /**
     * Gets response code returned by the application after handling the request.
     * @return Application's response code
     */
    public String getResponseCode() {
        return data.getResponseCode();
    }

    /**
     * Sets response code returned by the application after handling the request.
     * @param responseCode Application's response code
     */
    public void setResponseCode(String responseCode) {
        data.setResponseCode(responseCode);
    }

    /**
     * Gets a value indicating whether application handled the request successfully.
     * @return Success indication
     */
    public boolean isSuccess() {
        return data.getSuccess();
    }

    /**
     * Sets a value indicating whether application handled the request successfully.
     * @param success Success indication
     */
    public void setSuccess(boolean success) {
        data.setSuccess(success);
    }

    /**
     * Gets the amount of time it took the application to handle the request.
     * @return Amount of time in milliseconds
     */
    public Duration getDuration() {
        return data.getDuration();
    }

    /**
     * Sets the amount of time it took the application to handle the request.
     * @param duration Amount of time in captured in a {@link com.microsoft.applicationinsights.telemetry.Duration}.
     */
    public void setDuration(Duration duration) {
        data.setDuration(duration);
    }

    /**
     * Gets request url (optional).
     * @return The url
     * @throws MalformedURLException if the url is malformed
     */
    public URL getUrl() throws MalformedURLException {
        if (LocalStringsUtils.isNullOrEmpty(data.getUrl())) {
            return null;
        }

        return new URL(data.getUrl());
    }

    /**
     * Sets request url
     * @param url The URL
     */
    public void setUrl(URL url) {
        data.setUrl(url.toString());
    }

    /**
     * Sets request url.
     * @param url The url to store
     * @throws MalformedURLException If the url is malformed
     */
    public void setUrl(String url) throws MalformedURLException {
        URL u = new URL(url); // to validate and normalize
        data.setUrl(u.toString());
    }

    /**
     * Gets the HTTP method of the request.
     * @return The HTTP method
     */
    @Obsolete
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * Sets the HTTP method of the request.
     * @param httpMethod The HTTP method
     */
    @Obsolete
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
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
    protected void additionalSanitize() {
        data.setName(Sanitizer.sanitizeName(data.getName()));
        data.setId(Sanitizer.sanitizeName(data.getId()));
        Sanitizer.sanitizeMeasurements(getMetrics());
        Sanitizer.sanitizeUri(data.getUrl());
    }

    @Override
    protected RequestData getData() {
        return data;
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
