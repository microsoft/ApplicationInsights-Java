package com.microsoft.applicationinsights;

import com.azure.monitor.opentelemetry.exporter.implementation.models.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;

// naming convention:
// * MonitorDomain data
// * TelemetryItem telemetry
public class TelemetryUtil {

    public static TelemetryItem createMetricsTelemetry(String name, double value) {
        return createTelemetry(createMetricsData(name, value));
    }

    public static MetricsData createMetricsData(String name, double value) {
        MetricDataPoint point = new MetricDataPoint();
        point.setName(name);
        point.setValue(value);

        MetricsData data = new MetricsData();
        data.setMetrics(Collections.singletonList(point));
        return data;
    }

    public static TelemetryEventData createEventData(String name) {
        TelemetryEventData data = new TelemetryEventData();
        data.setName(name);
        return data;
    }

    public static MessageData createMessageData(String message) {
        MessageData data = new MessageData();
        data.setMessage(message);
        return data;
    }

    public static TelemetryItem createMessageTelemetry(String message) {
        return createTelemetry(createMessageData(message));
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

    public static TelemetryItem createTelemetry(MonitorDomain data) {
        MonitorBase base = new MonitorBase();
        base.setBaseData(data);
        base.setBaseType(getBaseType(data));

        TelemetryItem telemetry = new TelemetryItem();
        telemetry.setData(base);
        return telemetry;
    }

    private static String getBaseType(MonitorDomain data) {
        if (data instanceof AvailabilityData) {
            return "AvailabilityData"; // TODO (trask) is this right?
        } else if (data instanceof MessageData) {
            return "MessageData";
        } else if (data instanceof MetricsData) {
            return "MetricData";
        } else if (data instanceof PageViewData) {
            return "PageViewData";
        } else if (data instanceof PageViewPerfData) {
            return "PageViewPerfData"; // TODO (trask) is this right?
        } else if (data instanceof RemoteDependencyData) {
            return "RemoteDependencyData";
        } else if (data instanceof RequestData) {
            return "RequestData";
        } else if (data instanceof TelemetryEventData) {
            return "EventData";
        } else if (data instanceof TelemetryExceptionData) {
            return "ExceptionData";
        } else {
            throw new IllegalArgumentException("Unexpected type: " + data.getClass().getName());
        }
    }

    public static String currentTime() {
        return getFormattedDuration(System.currentTimeMillis());
    }

    public static String getFormattedDuration(long durationMillis) {
        Duration duration = Duration.ofMillis(durationMillis);
        return duration.toDays() + "." + duration.toHours() + ":" + duration.toMinutes() + ":" + duration.getSeconds()
                + "." + duration.toMillis();
    }
}
