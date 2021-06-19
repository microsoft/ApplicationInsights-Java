/*
 * ApplicationInsights-Java
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
package com.microsoft.applicationinsights.agent.internal.instrumentation.sdk;

import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.azure.monitor.opentelemetry.exporter.implementation.models.*;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.FormattedDuration;
import com.microsoft.applicationinsights.FormattedTime;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryUtil;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.BytecodeUtilDelegate;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingScoreGeneratorV2;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

// supporting all properties of event, metric, remove dependency and page view telemetry
public class BytecodeUtilImpl implements BytecodeUtilDelegate {

    private static final Logger logger = LoggerFactory.getLogger(BytecodeUtilImpl.class);

    private static final AtomicBoolean alreadyLoggedError = new AtomicBoolean();

    @Override
    public void trackEvent(Date timestamp, String name, Map<String, String> properties, Map<String, String> tags,
                           Map<String, Double> metrics, String instrumentationKey) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        TelemetryItem telemetry = new TelemetryItem();
        TelemetryEventData data = new TelemetryEventData();
        TelemetryClient.getActive().initEventTelemetry(telemetry, data);

        data.setName(name);
        data.setMeasurements(metrics);

        // FIXME (trask) azure sdk exporter: add properties to MonitorDomain, then this code can be shared
        if (!properties.isEmpty()) {
            Map<String, String> existingProperties = data.getProperties();
            if (existingProperties == null) {
                data.setProperties(properties);
            } else {
                existingProperties.putAll(properties);
            }
        }

        if (timestamp != null) {
            telemetry.setTime(FormattedTime.fromDate(timestamp));
        } else {
            telemetry.setTime(FormattedTime.fromNow());
        }
        selectivelySetTags(telemetry, tags);
        if (instrumentationKey != null) {
            telemetry.setInstrumentationKey(instrumentationKey);
        }

        track(telemetry);
    }

    // TODO do not track if perf counter (?)
    @Override
    public void trackMetric(Date timestamp, String name, double value, Integer count, Double min, Double max, Double stdDev,
                            Map<String, String> properties, Map<String, String> tags, String instrumentationKey) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        TelemetryItem telemetry = new TelemetryItem();
        MetricsData data = new MetricsData();
        MetricDataPoint point = new MetricDataPoint();
        TelemetryClient.getActive().initMetricTelemetry(telemetry, data, point);

        point.setName(name);
        point.setValue(value);
        point.setCount(count);
        point.setMin(min);
        point.setMax(max);
        point.setStdDev(stdDev);
        if (count != null || min != null || max != null || stdDev != null) {
            point.setDataPointType(DataPointType.AGGREGATION);
        } else {
            point.setDataPointType(DataPointType.MEASUREMENT);
        }

        if (!properties.isEmpty()) {
            Map<String, String> existingProperties = data.getProperties();
            if (existingProperties == null) {
                data.setProperties(properties);
            } else {
                existingProperties.putAll(properties);
            }
        }

        if (timestamp != null) {
            telemetry.setTime(FormattedTime.fromDate(timestamp));
        } else {
            telemetry.setTime(FormattedTime.fromNow());
        }
        selectivelySetTags(telemetry, tags);
        if (instrumentationKey != null) {
            telemetry.setInstrumentationKey(instrumentationKey);
        }

        track(telemetry);
    }

    @Override
    public void trackDependency(Date timestamp, String name, String id, String resultCode, @Nullable Long totalMillis,
                                boolean success, String commandName, String type, String target,
                                Map<String, String> properties, Map<String, String> tags, Map<String, Double> metrics,
                                String instrumentationKey) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        TelemetryItem telemetry = new TelemetryItem();
        RemoteDependencyData data = new RemoteDependencyData();
        TelemetryClient.getActive().initRemoteDependencyTelemetry(telemetry, data);

        data.setName(name);
        data.setId(id);
        data.setResultCode(resultCode);
        if (totalMillis != null) {
            data.setDuration(FormattedDuration.fromMillis(totalMillis));
        }
        data.setSuccess(success);
        data.setData(commandName);
        data.setType(type);
        data.setTarget(target);
        data.setMeasurements(metrics);

        if (!properties.isEmpty()) {
            Map<String, String> existingProperties = data.getProperties();
            if (existingProperties == null) {
                data.setProperties(properties);
            } else {
                existingProperties.putAll(properties);
            }
        }

        if (timestamp != null) {
            telemetry.setTime(FormattedTime.fromDate(timestamp));
        } else {
            telemetry.setTime(FormattedTime.fromNow());
        }
        selectivelySetTags(telemetry, tags);
        if (instrumentationKey != null) {
            telemetry.setInstrumentationKey(instrumentationKey);
        }

        track(telemetry);
    }

    @Override
    public void trackPageView(Date timestamp, String name, URI uri, long totalMillis, Map<String, String> properties,
                              Map<String, String> tags, Map<String, Double> metrics, String instrumentationKey) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        TelemetryItem telemetry = new TelemetryItem();
        PageViewData data = new PageViewData();
        TelemetryClient.getActive().initPageViewTelemetry(telemetry, data);

        data.setName(name);
        if (uri != null) {
            data.setUrl(uri.toString());
        }
        data.setDuration(FormattedDuration.fromMillis(totalMillis));
        data.setMeasurements(metrics);

        if (!properties.isEmpty()) {
            Map<String, String> existingProperties = data.getProperties();
            if (existingProperties == null) {
                data.setProperties(properties);
            } else {
                existingProperties.putAll(properties);
            }
        }

        if (timestamp != null) {
            telemetry.setTime(FormattedTime.fromDate(timestamp));
        } else {
            telemetry.setTime(FormattedTime.fromNow());
        }
        selectivelySetTags(telemetry, tags);
        if (instrumentationKey != null) {
            telemetry.setInstrumentationKey(instrumentationKey);
        }

        track(telemetry);
    }

    @Override
    public void trackTrace(Date timestamp, String message, int severityLevel, Map<String, String> properties, Map<String, String> tags,
                           String instrumentationKey) {
        if (Strings.isNullOrEmpty(message)) {
            return;
        }
        TelemetryItem telemetry = new TelemetryItem();
        MessageData data = new MessageData();
        TelemetryClient.getActive().initMessageTelemetry(telemetry, data);

        data.setMessage(message);
        if (severityLevel != -1) {
            data.setSeverityLevel(getSeverityLevel(severityLevel));
        }

        if (!properties.isEmpty()) {
            Map<String, String> existingProperties = data.getProperties();
            if (existingProperties == null) {
                data.setProperties(properties);
            } else {
                existingProperties.putAll(properties);
            }
        }

        if (timestamp != null) {
            telemetry.setTime(FormattedTime.fromDate(timestamp));
        } else {
            telemetry.setTime(FormattedTime.fromNow());
        }
        selectivelySetTags(telemetry, tags);
        if (instrumentationKey != null) {
            telemetry.setInstrumentationKey(instrumentationKey);
        }

        track(telemetry);
    }

    @Override
    public void trackRequest(String id, String name, URL url, Date timestamp, @Nullable Long duration, String responseCode, boolean success,
                             String source, Map<String, String> properties, Map<String, String> tags, Map<String, Double> metrics,
                             String instrumentationKey) {
        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        TelemetryItem telemetry = new TelemetryItem();
        RequestData data = new RequestData();
        TelemetryClient.getActive().initRequestTelemetry(telemetry, data);

        data.setId(id);
        data.setName(name);
        if (url != null) {
            data.setUrl(url.toString());
        }
        if (duration != null) {
            data.setDuration(FormattedDuration.fromMillis(duration));
        }
        data.setResponseCode(responseCode);
        data.setSuccess(success);
        data.setSource(source);
        data.setMeasurements(metrics);

        if (!properties.isEmpty()) {
            Map<String, String> existingProperties = data.getProperties();
            if (existingProperties == null) {
                data.setProperties(properties);
            } else {
                existingProperties.putAll(properties);
            }
        }

        if (timestamp != null) {
            telemetry.setTime(FormattedTime.fromDate(timestamp));
        } else {
            telemetry.setTime(FormattedTime.fromNow());
        }
        selectivelySetTags(telemetry, tags);
        if (instrumentationKey != null) {
            telemetry.setInstrumentationKey(instrumentationKey);
        }

        track(telemetry);
    }

    @Override
    public void trackException(Date timestamp, Exception exception, Map<String, String> properties, Map<String, String> tags,
                               Map<String, Double> metrics, String instrumentationKey) {
        if (exception == null) {
            return;
        }
        TelemetryItem telemetry = new TelemetryItem();
        TelemetryExceptionData data = new TelemetryExceptionData();
        TelemetryClient.getActive().initExceptionTelemetry(telemetry, data);

        data.setExceptions(TelemetryUtil.getExceptions(exception));
        data.setSeverityLevel(SeverityLevel.ERROR);
        data.setMeasurements(metrics);

        if (!properties.isEmpty()) {
            Map<String, String> existingProperties = data.getProperties();
            if (existingProperties == null) {
                data.setProperties(properties);
            } else {
                existingProperties.putAll(properties);
            }
        }

        if (timestamp != null) {
            telemetry.setTime(FormattedTime.fromDate(timestamp));
        } else {
            telemetry.setTime(FormattedTime.fromNow());
        }
        selectivelySetTags(telemetry, tags);
        if (instrumentationKey != null) {
            telemetry.setInstrumentationKey(instrumentationKey);
        }

        track(telemetry);
    }

    private SeverityLevel getSeverityLevel(int value) {
        // these mappings from the 2.x SDK
        switch (value) {
            case 0:
                return SeverityLevel.VERBOSE;
            case 1:
                return SeverityLevel.INFORMATION;
            case 2:
                return SeverityLevel.WARNING;
            case 3:
                return SeverityLevel.ERROR;
            case 4:
                return SeverityLevel.CRITICAL;
            default:
                return null;
        }
    }

    @Override
    public void flush() {
        // this is not null because sdk instrumentation is not added until Global.setTelemetryClient() is called
        checkNotNull(Global.getTelemetryClient()).flushChannelBatcher();
    }

    @Override
    public void logErrorOnce(Throwable t) {
        if (!alreadyLoggedError.getAndSet(true)) {
            logger.error(t.getMessage(), t);
        }
    }

    private static void track(TelemetryItem telemetry) {
        SpanContext context = Span.current().getSpanContext();
        float samplingPercentage;
        if (context.isValid()) {
            if (!context.isSampled()) {
                // sampled out
                return;
            }
            if (!telemetry.getTags().containsKey(ContextTagKeys.AI_OPERATION_ID.toString())) {
                telemetry.getTags().put(ContextTagKeys.AI_OPERATION_ID.toString(), context.getTraceId());
            }
            if (!telemetry.getTags().containsKey(ContextTagKeys.AI_OPERATION_PARENT_ID.toString())) {
                telemetry.getTags().put(ContextTagKeys.AI_OPERATION_PARENT_ID.toString(), context.getSpanId());
            }
            samplingPercentage =
                    TelemetryUtil.getSamplingPercentage(context.getTraceState(), Global.getSamplingPercentage(), false);
        } else {
            // sampling is done using the configured sampling percentage
            samplingPercentage = Global.getSamplingPercentage();
            if (!sample(telemetry, samplingPercentage)) {
                // sampled out
                return;
            }
        }
        // sampled in

        if (samplingPercentage != 100) {
            telemetry.setSampleRate(samplingPercentage);
        }
        // this is not null because sdk instrumentation is not added until Global.setTelemetryClient() is called
        checkNotNull(Global.getTelemetryClient()).trackAsync(telemetry);
    }

    private static boolean sample(TelemetryItem telemetry, double samplingPercentage) {
        if (samplingPercentage == 100) {
            return true;
        }
        if (SamplingScoreGeneratorV2.getSamplingScore(getOperationId(telemetry)) >= samplingPercentage) {
            logger.debug("Item {} sampled out", telemetry.getClass().getSimpleName());
            return false;
        }
        return true;
    }

    private static String getOperationId(TelemetryItem telemetry) {
        return telemetry.getTags().get(ContextTagKeys.AI_OPERATION_ID.toString());
    }

    private static void selectivelySetTags(TelemetryItem telemetry, Map<String, String> sourceTags) {
        Map<String, String> destTags = telemetry.getTags();
        for (Map.Entry<String, String> entry : sourceTags.entrySet()) {
            if (!entry.getKey().equals(ContextTagKeys.AI_INTERNAL_SDK_VERSION.toString())) {
                destTags.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
