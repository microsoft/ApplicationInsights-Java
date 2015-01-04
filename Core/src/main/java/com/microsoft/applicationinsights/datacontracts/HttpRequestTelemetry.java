package com.microsoft.applicationinsights.datacontracts;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import com.microsoft.applicationinsights.implementation.schemav2.RequestData;
import com.microsoft.applicationinsights.util.LocalStringsUtils;
import com.microsoft.applicationinsights.util.MapUtil;

import com.google.common.base.Strings;

/**
 * Telemetry used to track events.
 */
public class HttpRequestTelemetry extends BaseTelemetry<RequestData> {
    private final RequestData data;

    /**
     * Initializes a new instance of the RequestTelemetry class.
     */
    public HttpRequestTelemetry() {
        super();
        this.data = new RequestData();
        initialize(this.data.getProperties());
        setId(LocalStringsUtils.generateRandomId());
        this.setResponseCode("200");
    }

    /**
     * Initializes a new instance of the RequestTelemetry class with the given name,
     * time stamp, duration, HTTP response code and success property values.
     * @param name A user-friendly name for the request.
     * @param timestamp The time of the request.
     * @param duration The duration, in milliseconds, of the request processing.
     * @param responseCode The HTTP response code.
     * @param success 'true' if the request was a success, 'false' otherwise.
     */
    public HttpRequestTelemetry(String name, Date timestamp, long duration, String responseCode, boolean success) {
        this();
        this.setName(name);
        this.setDuration(duration);
        this.setResponseCode(responseCode);
        this.setSuccess(success);

        this.setTimestamp(timestamp);
        this.data.setStartTime(LocalStringsUtils.getDateFormatter().format(timestamp));
    }

    Map<String, Double> getMetrics() {
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

    public String getId()
    {
        return this.data.getId();
    }

    public void setId(String id) {
        this.data.setId(id);
    }

    public String getResponseCode() {
        return this.data.getResponseCode();
    }

    public void setResponseCode(String responseCode) {
        // Validate
        int code = Integer.parseInt(responseCode);
        setSuccess(code < 400);
        this.data.setResponseCode(responseCode);
    }

    public boolean isSuccess() {
        return this.data.getSuccess();
    }

    public void setSuccess(boolean success) {
        this.data.setSuccess(success);
    }

    public void setDuration(long milliSeconds) {
        this.data.setDuration(milliSeconds);
    }

    public URL getUrl() throws MalformedURLException {
        if (LocalStringsUtils.isNullOrEmpty(this.data.getUrl())) {
            return null;
        }

        return new URL(this.data.getUrl());
    }

    public void setUrl(URL url) {
        this.data.setUrl(url.toString());
    }

    public void setUrl(String url) throws MalformedURLException {
        URL u = new URL(url); // to validate and normalize
        this.data.setUrl(u.toString());
    }

    public String getHttpMethod() {
        return this.data.getHttpMethod();
    }

    public void setHttpMethod(String httpMethod) {
        this.data.setHttpMethod(httpMethod);
    }

    @Override
    protected void additionalSanitize() {
        this.data.setName(LocalStringsUtils.sanitize(this.data.getName(), 1024));
        MapUtil.sanitizeMeasurements(this.getMetrics());
    }

    @Override
    protected RequestData getData() {
        return data;
    }
}
