package com.microsoft.applicationinsights.telemetry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.util.Sanitizer;

/**
 * Encapsulates information about a web request handled by the application.
 *
 * You can send information about requests processed by your web application to Application Insights by
 * passing an instance of this class to the 'trackHttpRequest' method of the {@link com.microsoft.applicationinsights.TelemetryClient}
 */
public final class HttpRequestTelemetry extends BaseTelemetry<RequestData> {
    private final RequestData data;


    /**
     * Initializes a new instance of the HttpRequestTelemetry class with the given name,
     * time stamp, duration, HTTP response code and success property values.
     * @param name A user-friendly name for the request.
     * @param timestamp The time of the request.
     * @param duration The duration, in milliseconds, of the request processing.
     * @param responseCode The HTTP response code.
     * @param success 'true' if the request was a success, 'false' otherwise.
     */
    public HttpRequestTelemetry(String name, Date timestamp, long duration, String responseCode, boolean success) {
        this.data = new RequestData();
        initialize(this.data.getProperties());

        setId(LocalStringsUtils.generateRandomId());
        this.setResponseCode("200");

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
    Map<String, Double> getMetrics() {
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
        data.setStartTime(timestamp);
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
        // Validate
        int code = Integer.parseInt(responseCode);
        setSuccess(code < 400);
        data.setResponseCode(responseCode);
    }

    /**
     * Gets a value indicating whether application handled the request successfully.
     * @return Success indication
     */
    public boolean isSuccess() {
        return data.isSuccess();
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
    public long getDuration() {
        return data.getDuration();
    }

    /**
     * Sets the amount of time it took the application to handle the request.
     * @param milliSeconds Amount of time in milliseconds.
     */
    public void setDuration(long milliSeconds) {
        data.setDuration(milliSeconds);
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
    public String getHttpMethod() {
        return data.getHttpMethod();
    }

    /**
     * Sets the HTTP method of the request.
     * @param httpMethod The HTTP method
     */
    public void setHttpMethod(String httpMethod) {
        data.setHttpMethod(httpMethod);
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
}
