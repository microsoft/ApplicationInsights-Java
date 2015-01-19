package com.microsoft.applicationinsights;

import java.util.Date;
import java.util.Map;

import com.microsoft.applicationinsights.telemetry.*;
import com.microsoft.applicationinsights.channel.TelemetryChannel;

/**
 * Created by gupele on 1/5/2015.
 */
public interface TelemetryClient {
    TelemetryContext getContext();

    void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics);

    void trackEvent(String name);

    void trackEvent(EventTelemetry telemetry);

    void trackTrace(String message, Map<String, String> properties);

    void trackTrace(String message);

    void trackTrace(TraceTelemetry telemetry);

    void trackMetric(String name, double value, int sampleCount, double min, double max, Map<String, String> properties);

    void trackMetric(String name, double value);

    void trackMetric(MetricTelemetry telemetry);

    void trackException(Exception exception, Map<String, String> properties, Map<String, Double> metrics);

    void trackException(Exception exception);

    void trackException(ExceptionTelemetry telemetry);

    void trackHttpRequest(String name, Date timestamp, long duration, String responseCode, boolean success);

    void trackHttpRequest(HttpRequestTelemetry request);

    void trackRemoteDependency(RemoteDependencyTelemetry telemetry);

    void trackPageView(String name);

    void trackPageView(PageViewTelemetry telemetry);

    void track(Telemetry telemetry);
}
