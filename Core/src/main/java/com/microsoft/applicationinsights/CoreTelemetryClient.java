package com.microsoft.applicationinsights;

import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.ITelemetryContext;
import com.microsoft.applicationinsights.channel.contracts.DataPoint;
import com.microsoft.applicationinsights.channel.contracts.DataPointType;
import com.microsoft.applicationinsights.channel.contracts.EventData;
import com.microsoft.applicationinsights.channel.contracts.ExceptionData;
import com.microsoft.applicationinsights.channel.contracts.MessageData;
import com.microsoft.applicationinsights.channel.contracts.MetricData;
import com.microsoft.applicationinsights.channel.contracts.shared.ITelemetry;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The public API for recording application insights telemetry.
 * Users would call TelemetryClient.track*
 */
public class CoreTelemetryClient {

    /**
     * The configuration for this telemetry client.
     */
    public final CoreTelemetryClientConfig config;

    /**
     * The telemetry telemetryContext object.
     */
    protected ITelemetryContext telemetryContext;

    /**
     * The telemetry channel for this client.
     */
    protected TelemetryChannel channel;

    /**
     * Force inheritance via a protected constructor
     */
    public CoreTelemetryClient(CoreTelemetryClientConfig config) {
        this.config = config;
    }

    /**
     * track the event by name.
     *
     * @param eventName
     */
    public void trackEvent(String eventName) {
        trackEvent(eventName, null, null);
    }

    /**
     * track the event by name.
     *
     * @param eventName
     */
    public void trackEvent(String eventName, HashMap<String, String> properties) {
        trackEvent(eventName, properties, null);
    }

    /**
     * Track the event by event name and customized properties and metrics.
     *
     * @param eventName  event name
     * @param properties customized properties
     * @param metrics    customized metrics
     */
    public void trackEvent(String eventName,
                           HashMap<String, String> properties,
                           HashMap<String, Double> metrics) {
        String localEventName = eventName;
        EventData telemetry = new EventData();
        telemetry.setName(localEventName);
        telemetry.setProperties(properties);
        telemetry.setMeasurements(metrics);

        track(telemetry, EventData.EnvelopeName, EventData.BaseType);
    }

    /**
     * track with the message.
     *
     * @param message message for transmission to Application insight
     */
    public void trackTrace(String message) {
        trackTrace(message, null);
    }

    /**
     * track with the message and properties.
     *
     * @param message    message for transmission to Application insight
     * @param properties properties of the message
     */
    public void trackTrace(String message, HashMap<String, String> properties) {
        MessageData telemetry = new MessageData();
        telemetry.setMessage(message);
        telemetry.setProperties(properties);

        track(telemetry, MessageData.EnvelopeName, MessageData.BaseType);
    }

    /**
     * track the metric.
     *
     * @param name  name of the metric
     * @param value value of the metric
     */
    public void trackMetric(String name, Double value) {
        this.trackMetric(name, value, null);
    }

    /**
     * Track the metric with properties.
     *
     * @param name       metric name
     * @param value      metric value
     * @param properties metric properties
     */
    public void trackMetric(String name, double value, HashMap<String, String> properties) {
        MetricData telemetry = new MetricData();
        telemetry.setProperties(properties);

        // todo: batch in client
        DataPoint data = new DataPoint();
        data.setCount(1);
        data.setKind(DataPointType.Measurement);
        data.setMax(value);
        data.setMax(value);
        data.setName(name);
        data.setValue(value);
        ArrayList<DataPoint> list = new ArrayList<DataPoint>();
        list.add(data);
        telemetry.setMetrics(list);

        track(telemetry, MetricData.EnvelopeName, MetricData.BaseType);
    }

    /**
     * Track exception with properties.
     *
     * @param exception exception data object
     */
    public void trackException(ExceptionData exception) {
        this.trackException(exception, null);
    }

    /**
     * Track exception with properties.
     *
     * @param exception  exception data object
     * @param properties exception properties
     */
    public void trackException(ExceptionData exception, HashMap<String, String> properties) {
        ExceptionData localException = exception;
        if (localException == null) {
            localException = new ExceptionData();
        }

        exception.setProperties(properties);

        track(localException, ExceptionData.EnvelopeName, ExceptionData.BaseType);
    }

    /**
     * send message to the recorder.
     *
     * @param telemetry    telemetry object
     * @param itemDataType data type
     * @param itemType     item type
     */
    protected void track(ITelemetry telemetry, String itemDataType, String itemType) {
        this.channel.send(this.telemetryContext, telemetry, itemDataType, itemType);
    }
}
