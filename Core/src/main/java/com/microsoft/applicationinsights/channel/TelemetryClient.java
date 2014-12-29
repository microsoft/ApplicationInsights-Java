package com.microsoft.applicationinsights.channel;

import com.microsoft.applicationinsights.datacontracts.EventTelemetry;
import com.microsoft.applicationinsights.datacontracts.MetricTelemetry;
import com.microsoft.applicationinsights.datacontracts.TelemetryContext;
import com.microsoft.applicationinsights.datacontracts.TraceTelemetry;

import java.util.Date;
import java.util.Map;

/**
 * Created by gupele on 12/28/2014.
 */
public interface TelemetryClient {
    void setChannel(TelemetryChannel channel);

    TelemetryChannel getChannel();

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

    void trackHttpRequest(String name, Date timestamp, long duration, String responseCode, boolean success);

    void track(Telemetry telemetry);
}
