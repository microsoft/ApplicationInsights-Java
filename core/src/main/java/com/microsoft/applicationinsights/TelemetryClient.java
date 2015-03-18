/*
 * AppInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights;

import java.util.Date;
import java.util.Map;

import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.ChannelFetcher;
import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.SessionState;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionHandledAt;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SessionStateTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.channel.TelemetryChannel;

import com.google.common.base.Strings;

/**
 * Created by gupele on 1/5/2015.
 */
public class TelemetryClient {
    private final class TelemetryClientChannelFetcher implements ChannelFetcher {
        public TelemetryChannel fetch() {
            return getChannel();
        }
    }

    private final TelemetryConfiguration configuration;
    private TelemetryContext context;
    private TelemetryChannel channel;

    private static final Object TELEMETRY_STOP_HOOK_LOCK = new Object();

    /**
     * Initializes a new instance of the TelemetryClient class. Send telemetry with the specified configuration.
     * @param configuration The configuration this instance will work with.
     */
    public TelemetryClient(TelemetryConfiguration configuration) {
        if (configuration == null) {
            configuration = TelemetryConfiguration.getActive();
        }

        synchronized (TELEMETRY_STOP_HOOK_LOCK) {
            SDKShutdownActivity.INSTANCE.register(new TelemetryClientChannelFetcher());
        }

        this.configuration = configuration;
    }

    /**
     * Initializes a new instance of the TelemetryClient class, configured from the active configuration.
     */
    public TelemetryClient() {
        this(TelemetryConfiguration.getActive());
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
        return
                Strings.isNullOrEmpty(getContext().getInstrumentationKey()) || configuration.isTrackingDisabled();
    }

    /**
     * Sends the specified state of a user session to Application Insights.
     * @param sessionState {@link com.microsoft.applicationinsights.telemetry.SessionState}
     *                     value indicating the state of a user session.
     */
    public void trackSessionState(SessionState sessionState) {
        this.track(new SessionStateTelemetry(sessionState));
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
     * @param severityLevel The severity level.
     * @param properties Named string values you can use to search and classify trace messages.
     */
    public void trackTrace(String message, SeverityLevel severityLevel, Map<String, String> properties) {
        if (isDisabled()) {
            return;
        }

        if (Strings.isNullOrEmpty(message)) {
            message = "";
        }

        TraceTelemetry et = new TraceTelemetry(message, severityLevel);

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
        trackTrace(message, null, null);
    }

    public void trackTrace(String message, SeverityLevel severityLevel) {
        trackTrace(message, severityLevel, null);
    }

    /**
     * Sends a TraceTelemetry record for display in Diagnostic Search.
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} instance.
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

        this.track(mt);
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
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} instance.
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

        track(new RequestTelemetry(name, timestamp, duration, responseCode, success));
    }

    public void trackRequest(RequestTelemetry request) {
        track(request);
    }

    public void trackRemoteDependency(RemoteDependencyTelemetry telemetry) {
        if (telemetry == null) {
            telemetry = new RemoteDependencyTelemetry("");
        }

        track(telemetry);
    }

    public void trackPageView(String name) {
        // Avoid creation of data if not needed
        if (isDisabled()) {
            return;
        }

        if (name == null) {
            name = "";
        }

        Telemetry telemetry = new PageViewTelemetry(name);
        track(telemetry);
    }

    /**
     * Send information about the page viewed in the application.
     * @param telemetry The telemetry to send
     */
    public void trackPageView(PageViewTelemetry telemetry) {
        track(telemetry);
    }

    /**
     * This method is part of the Application Insights infrastructure. Do not call it directly.
     * @param telemetry The {@link com.microsoft.applicationinsights.telemetry.Telemetry} instance.
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

        try {
            telemetry.getContext().initialize(ctx);
        } catch (Throwable t) {
            InternalLogger.INSTANCE.error("Exception while telemetry context's initialization: '%s'", t.getMessage());
        }

        for (TelemetryInitializer initializer : this.configuration.getTelemetryInitializers()) {
            try {
                initializer.initialize(telemetry);
            } catch (Throwable e) {
                InternalLogger.INSTANCE.error("Failed during telemetry initialization, exception: %s", e.getMessage());
            }
        }

        if (Strings.isNullOrEmpty(telemetry.getContext().getInstrumentationKey())) {
            throw new IllegalArgumentException("Instrumentation key cannot be undefined.");
        }

        try {
            telemetry.sanitize();
        } catch (Throwable t) {
            InternalLogger.INSTANCE.error("Exception while sanitizing telemetry: '%s'",t.getMessage());
        }

        getChannel().send(telemetry);
    }

    /**
     * Flushes possible pending Telemetries in the channel.
     */
    public void flush() {
        getChannel().flush();
    }

    /**
     * Gets the channel used by the client.
     */
    TelemetryChannel getChannel() {
        if (channel == null) {
            this.channel = configuration.getChannel();
        }

        return this.channel;
    }

    private TelemetryContext createInitializedContext() {
        TelemetryContext ctx = new TelemetryContext();
        ctx.setInstrumentationKey(configuration.getInstrumentationKey());
        for (ContextInitializer init : configuration.getContextInitializers()) {
            try {
                init.initialize(ctx);
            } catch (Throwable t) {
                InternalLogger.INSTANCE.error("Exception in context initializer: '%s'", t.getMessage());
            }
        }

        return ctx;
    }
}
