package com.microsoft.applicationinsights.telemetry;

import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;

import com.microsoft.applicationinsights.internal.schemav2.RequestData;

/**
 * Created by gupele on 1/13/2015.
 */
public final class RequestTelemetry extends BaseTelemetry<RequestData> {
    private final static String DEFAULT_RESPONSE_CODE = "200";

    private final static Random s_random = new Random();

    private final RequestData data;
    private ConcurrentMap<String, Double> metrics;

    public RequestTelemetry() {
        data = new RequestData();
        initialize(data.getProperties());

        // Initialize required fields

        // First solution for Random numbers, we might need to change it in future releases
        data.setId(String.valueOf(s_random.nextInt()));
        data.setResponseCode(DEFAULT_RESPONSE_CODE);
        data.setSuccess(true);
    }

    public RequestTelemetry(String name, Date timestamp, Long duration, String responseCode, boolean success) {
        this();

        // Name is optional but without it UX does not make much sense
        data.setName(name);
        setTimestamp(timestamp);
        data.setDuration(duration);
        data.setResponseCode(responseCode);
        data.setSuccess(success);
    }

    @Override
    public void setTimestamp(Date date) {
        super.setTimestamp(date);
        data.setStartTime(date);
    }

    public String getId() {
        return data.getId();
    }

    public void setId(String id) {
        data.setId(id);
    }

    public String getName() {
        return data.getName();
    }

    public void setName(String name) {
        data.setName(name);
    }

    public String getResponseCode() {
        return data.getResponseCode();
    }

    public void setResponseCode(String responseCode) {
        data.setResponseCode(responseCode);
    }

    public Long getDuration() {
        return data.getDuration();
    }

    public void setDuration(Long duration) {
        data.setDuration(duration);
    }

    public String getHttpMethod() {
        return data.getHttpMethod();
    }

    public void setHttpMethod(String httpMethod) {
        data.setHttpMethod(httpMethod);
    }

    public boolean isSuccess() {
        return data.isSuccess();
    }

    public void setSuccess(boolean isSuccess) {
        data.setSuccess(isSuccess);
    }

    public URI getUrl() {
        URI result = Sanitizer.safeStringToUri(data.getUrl());
        if (result == null) {
            data.setUrl(null);
        }

        return result;
    }

    public void setUrl(URL url) {
        data.setUrl(url == null ? null : url.getPath());
    }

    public ConcurrentMap<String, Double> getMetrics() {
        return metrics;
    }

    public void setMetrics(ConcurrentMap<String, Double> metrics) {
        this.metrics = metrics;
    }

    @Override
    protected void additionalSanitize() {
        data.setName(Sanitizer.sanitizeName(this.data.getName()));
        Sanitizer.sanitizeMeasurements(this.getMetrics());
        data.setId(Sanitizer.sanitizeName(this.data.getId()));
        Sanitizer.sanitizeUri(data.getUrl());
    }

    @Override
    protected RequestData getData() {
        return data;
    }
}
