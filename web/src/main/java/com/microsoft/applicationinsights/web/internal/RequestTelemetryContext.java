package com.microsoft.applicationinsights.web.internal;

import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;

/**
 * Created by yonisha on 2/2/2015.
 */
public class RequestTelemetryContext {
    private HttpRequestTelemetry requestTelemetry;
    private long requestStartTimeTicks;

    public static final String CONTEXT_ATTR_KEY = "CONTEXT_ATTR";

    /**
     * Constructs new RequestTelemetryContext object.
     * @param ticks The time in ticks
     */
    public RequestTelemetryContext(long ticks) {
        requestTelemetry = new HttpRequestTelemetry();
        requestStartTimeTicks = ticks;
    }

    /**
     * Gets the http request telemetry associated with the context.
     * @return The http request telemetry.
     */
    public HttpRequestTelemetry getHttpRequestTelemetry() {
        return requestTelemetry;
    }

    /**
     * Gets the request start time in ticks
     * @return Request start time in ticks
     */
    public long getRequestStartTimeTicks() {
        return requestStartTimeTicks;
    }
}
