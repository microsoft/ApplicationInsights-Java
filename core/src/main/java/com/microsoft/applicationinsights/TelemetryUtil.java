package com.microsoft.applicationinsights;

import com.azure.monitor.opentelemetry.exporter.implementation.models.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;

// naming convention:
// * MonitorDomain data
// * TelemetryItem telemetry
public class TelemetryUtil {

    public static TelemetryItem createTelemetry(MonitorDomain data, String instrumentationKey) {

    }

    public static MetricsData createMetricsData(String name, double value) {
        MetricDataPoint point = new MetricDataPoint();
        point.setName(name);
        point.setValue(value);
        MetricsData data = new MetricsData();
        data.setMetrics(Collections.singletonList(point));
        return data;
    }

    public static MessageData createMessageData(String message) {
        MessageData data = new MessageData();
        data.setMessage(message);
        return data;
    }

    public static RequestData createRequestData(String name, Date timestamp, long duration, String responseCode, boolean success) {
        RequestData data = new RequestData();
        data.setName(name);
        //data.setMessage(message);
        return data;
    }

    public static RemoteDependencyData createRemoteDependencyData(String name, String command, long durationMillis, boolean success) {
        RemoteDependencyData data = new RemoteDependencyData();
        data.setName(name);
        data.setData(command);
        data.setDuration(getFormattedDuration(durationMillis));
        data.setSuccess(success);
        return data;
    }

    private static String getFormattedDuration(long durationMillis) {
        Duration duration = Duration.ofMillis(durationMillis);
        return duration.toDays() + "." + duration.toHours() + ":" + duration.toMinutes() + ":" + duration.getSeconds()
                + "." + duration.toMillis();
    }
}
