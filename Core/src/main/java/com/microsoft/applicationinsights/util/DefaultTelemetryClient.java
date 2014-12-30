package com.microsoft.applicationinsights.util;

import java.util.Date;
import java.util.Map;

import com.microsoft.applicationinsights.channel.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.TelemetryClient;
import com.microsoft.applicationinsights.datacontracts.TelemetryContext;
import com.microsoft.applicationinsights.datacontracts.TraceTelemetry;
import com.microsoft.applicationinsights.datacontracts.EventTelemetry;
import com.microsoft.applicationinsights.datacontracts.MetricTelemetry;
import com.microsoft.applicationinsights.datacontracts.ExceptionTelemetry;
import com.microsoft.applicationinsights.datacontracts.ExceptionHandledAt;
import com.microsoft.applicationinsights.datacontracts.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.datacontracts.HttpRequestTelemetry;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryConfiguration;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;

/**
 * Created by gupele on 12/28/2014.
 */
public class DefaultTelemetryClient implements TelemetryClient {
    private final TelemetryConfiguration configuration;
    private TelemetryContext context;
    private TelemetryChannel channel;

    /**
     * Initializes a new instance of the TelemetryClient class. Send telemetry with the specified configuration.
     */
    public DefaultTelemetryClient(TelemetryConfiguration configuration) {
        if (configuration == null)
            configuration = TelemetryConfiguration.getActive();

        this.configuration = configuration;
    }

    /**
     * Initializes a new instance of the TelemetryClient class, configured from the active configuration.
     */
    public DefaultTelemetryClient() {
        this(TelemetryConfiguration.getActive());
    }

    /**
     * Gets the channel used by the client.
     */
    public TelemetryChannel getChannel() {
        if (channel == null) {
            this.channel = configuration.getChannel();
        }

        return this.channel;
    }

    /**
     * Sets the channel used by the client.
     */
    public void setChannel(TelemetryChannel channel) {
        this.channel = channel;
    }

    /**
     * Gets the current context that will be used to augment telemetry you send.
     * @return A telemetry context used for all records. Changes to it will impact all future telemetry in this
     * application session.
     */
    public TelemetryContext getContext() {
        if (context == null) {
            context = createInitializedContext();
        }

        return context;
    }

    /**
     * Checks whether tracking is enabled or not.
     * @return 'true' if tracking is disabled, 'false' otherwise.
     */
    public boolean isDisabled() {
        return configuration.isTrackingDisabled();
    }

    /**
     * Sends an EventTelemetry record for display in Diagnostic Search and aggregation in Metrics Explorer.
     * @param name A name for the event.
     * @param properties Named string values you can use to search and classify events.
     * @param metrics Measurements associated with this event.
     */
    public void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
        if (isDisabled()) {
            return;
        }

        if (Strings.isNullOrEmpty(name)) {
            name = "";
        }

        EventTelemetry et = new EventTelemetry(name);

        if (properties != null && properties.size() > 0) {
            MapUtil.copy(properties, et.getContext().getProperties());
        }

        if (metrics != null && metrics.size() > 0) {
            MapUtil.copy(metrics, et.getMetrics());
        }

        this.track(et);
    }

    /**
     * Sends an EventTelemetry record for display in Diagnostic Search and aggregation in Metrics Explorer.
     * @param name A name for the event.
     */
    public void trackEvent(String name) {
        trackEvent(name, null, null);
    }

    /**
     * Sends an EventTelemetry record for display in Diagnostic Search and aggregation in Metrics Explorer.
     * @param telemetry An event log item.
     */
    public void trackEvent(EventTelemetry telemetry) {
        track(telemetry);
    }

    /**
     * Sends a TraceTelemetry record for display in Diagnostic Search.
     * @param message A log message.
     * @param properties Named string values you can use to search and classify trace messages.
     */
    public void trackTrace(String message, Map<String, String> properties) {
        if (isDisabled()) {
            return;
        }

        if (Strings.isNullOrEmpty(message)) {
            message = "";
        }

        TraceTelemetry et = new TraceTelemetry(message);

        if (properties != null && properties.size() > 0) {
            MapUtil.copy(properties, et.getContext().getProperties());
        }

        this.track(et);
    }

    /**
     * Sends a TraceTelemetry record for display in Diagnostic Search.
     * @param message A log message.
     */
    public void trackTrace(String message) {
        trackTrace(message, null);
    }

    /**
     * Sends a TraceTelemetry record for display in Diagnostic Search.
     */
    public void trackTrace(TraceTelemetry telemetry) {
        this.track(telemetry);
    }

    /**
     * Sends a MetricTelemetry record for aggregation in Metric Explorer.
     * @param name The name of the measurement
     * @param value The value of the measurement. Average if based on more than one sample count.
     * @param sampleCount The sample count.
     * @param min The minimum value of the sample.
     * @param max The maximum value of the sample.
     * @param properties Named string values you can use to search and classify trace messages.
     */
    public void trackMetric(String name, double value, int sampleCount, double min, double max, Map<String, String> properties) {
        if (isDisabled()) {
            return;
        }

        if (Strings.isNullOrEmpty(name)) {
            name = "";
        }

        MetricTelemetry mt = new MetricTelemetry(name, value);
        mt.setCount(sampleCount);
        if (sampleCount > 1) {
            mt.setMin(min);
            mt.setMax(max);
        }

        if (properties != null && properties.size() > 0) {
            MapUtil.copy(properties, mt.getContext().getProperties());
        }
    }

    /**
     * Sends a MetricTelemetry record for aggregation in Metric Explorer.
     * @param name The name of the measurement
     * @param value The value of the measurement.
     */
    public void trackMetric(String name, double value) {
        trackMetric(name, value, 1, value, value, null);
    }

    /**
     * Send a MetricTelemetry record for aggregation in Metric Explorer.
     */
    public void trackMetric(MetricTelemetry telemetry) {
        track(telemetry);
    }

    /**
     * Sends an ExceptionTelemetry record for display in Diagnostic Search.
     * @param exception The exception to log information about.
     * @param properties Named string values you can use to search and classify trace messages.
     * @param metrics Measurements associated with this exception event.
     */
    public void trackException(Exception exception, Map<String, String> properties, Map<String, Double> metrics) {
        if (isDisabled()) {
            return;
        }

        ExceptionTelemetry et = new ExceptionTelemetry(exception);
        et.setExceptionHandledAt(ExceptionHandledAt.UserCode);

        if (properties != null && properties.size() > 0) {
            MapUtil.copy(properties, et.getContext().getProperties());
        }

        if (metrics != null && metrics.size() > 0) {
            MapUtil.copy(metrics, et.getMetrics());
        }

        this.track(et);
    }

    /**
     * Sends an ExceptionTelemetry record for display in Diagnostic Search.
     * @param exception The exception to log information about.
     */
    public void trackException(Exception exception) {
        trackException(exception, null, null);
    }

    /**
     * Sends an ExceptionTelemetry record for display in Diagnostic Search.
     * @param telemetry An already constructed exception telemetry record.
     */
    public void trackException(ExceptionTelemetry telemetry) {
        track(telemetry);
    }

    /**
     * Sends a HttpRequestTelemetry record for display in Diagnostic Search.
     * @param name A user-friendly name for the request.
     * @param timestamp The time of the request.
     * @param duration The duration, in milliseconds, of the request processing.
     * @param responseCode The HTTP response code.
     * @param success 'true' if the request was a success, 'false' otherwise.
     */
    public void trackHttpRequest(String name, Date timestamp, long duration, String responseCode, boolean success) {
        if (isDisabled()) {
            return;
        }

        track(new HttpRequestTelemetry(name, timestamp, duration, responseCode, success));
    }

    public void trackHttpRequest(HttpRequestTelemetry request) {
        track(request);
    }

    public void trackRemoteDependency(RemoteDependencyTelemetry telemetry) {
        if (telemetry == null) {
            telemetry = new RemoteDependencyTelemetry("");
        }

        track(telemetry);
    }

    /**
     * This method is part of the Application Insights infrastructure. Do not call it directly.
     */
    public void track(Telemetry telemetry) {
        if (telemetry == null) {
            throw new IllegalArgumentException("telemetry item cannot be null");
        }

        if (isDisabled()) {
            return;
        }

        telemetry.setTimestamp(new Date());

        TelemetryContext ctx = this.getContext();

        if (Strings.isNullOrEmpty(ctx.getInstrumentationKey())) {
            ctx.setInstrumentationKey(configuration.getInstrumentationKey());
        }

        telemetry.getContext().Initialize(ctx);

        for (TelemetryInitializer initializer : this.configuration.getTelemetryInitializers()) {
            try {
                initializer.Initialize(telemetry);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (Strings.isNullOrEmpty(telemetry.getContext().getInstrumentationKey())) {
            throw new IllegalArgumentException("Instrumentation key cannot be undefined.");
        }

        telemetry.sanitize();

        getChannel().send(telemetry);
    }

    private TelemetryContext createInitializedContext() {
        TelemetryContext ctx = new TelemetryContext();
        ctx.setInstrumentationKey(configuration.getInstrumentationKey());
        for (ContextInitializer init : configuration.getContextInitializers()) {
            init.Initialize(ctx);
        }

        return ctx;
    }
}
