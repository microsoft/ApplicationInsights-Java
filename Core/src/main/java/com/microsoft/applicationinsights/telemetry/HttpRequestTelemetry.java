package com.microsoft.applicationinsights.telemetry;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;

import com.google.common.base.Strings;

/**
 * Telemetry used to track events.
 */
public final class HttpRequestTelemetry extends BaseTelemetry<RequestData> {
    private final RequestData data;


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
        this.data = new RequestData();
        initialize(this.data.getProperties());

        setId(LocalStringsUtils.generateRandomId());
        this.setResponseCode("200");
        if (timestamp == null) {
            timestamp = new Date();
        }

        setTimestamp(timestamp);
        data.setStartTime(timestamp);

        setName(name);
        setDuration(duration);
        setResponseCode(responseCode);
        setSuccess(success);
    }

    Map<String, Double> getMetrics() {
        return data.getMeasurements();
    }


    public String getName() {
        return data.getName();
    }

    public void setName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("The event name cannot be null or empty");
        }
        data.setName(name);
    }

    public String getId()
    {
        return data.getId();
    }

    public void setId(String id) {
        data.setId(id);
    }

    public String getResponseCode() {
        return data.getResponseCode();
    }

    public void setResponseCode(String responseCode) {
        // Validate
        int code = Integer.parseInt(responseCode);
        setSuccess(code < 400);
        data.setResponseCode(responseCode);
    }

    public boolean isSuccess() {
        return data.isSuccess();
    }

    public void setSuccess(boolean success) {
        data.setSuccess(success);
    }

    public void setDuration(long milliSeconds) {
        data.setDuration(milliSeconds);
    }

    public URL getUrl() throws MalformedURLException {
        if (LocalStringsUtils.isNullOrEmpty(data.getUrl())) {
            return null;
        }

        return new URL(data.getUrl());
    }

    public void setUrl(URL url) {
        data.setUrl(url.toString());
    }

    public void setUrl(String url) throws MalformedURLException {
        URL u = new URL(url); // to validate and normalize
        data.setUrl(u.toString());
    }

    public String getHttpMethod() {
        return data.getHttpMethod();
    }

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
