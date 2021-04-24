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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.azure.monitor.opentelemetry.exporter.implementation.models.*;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.microsoft.applicationinsights.TelemetryUtil;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.BytecodeUtilDelegate;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingScoreGeneratorV2;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

// supporting all properties of event, metric, remove dependency and page view telemetry
public class BytecodeUtilImpl implements BytecodeUtilDelegate {

    private static final Logger logger = LoggerFactory.getLogger(BytecodeUtilImpl.class);

    private static final AtomicBoolean alreadyLoggedError = new AtomicBoolean();

    @Override
    public void trackEvent(String name, Map<String, String> properties, Map<String, String> tags,
                           Map<String, Double> metrics, String instrumentationKey) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        TelemetryEventData data = new TelemetryEventData();
        data.setName(name);
        data.getProperties().putAll(properties);
        data.getMeasurements().putAll(metrics);

        TelemetryItem telemetry = TelemetryUtil.createTelemetry(data);
        telemetry.getTags().putAll(tags);
        telemetry.setInstrumentationKey(instrumentationKey);

        track(telemetry);
    }

    // TODO do not track if perf counter (?)
    @Override
    public void trackMetric(String name, double value, Integer count, Double min, Double max, Double stdDev,
                            Map<String, String> properties, Map<String, String> tags, String instrumentationKey) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        MetricDataPoint point = new MetricDataPoint();
        point.setName(name);
        point.setValue(value);
        point.setCount(count);
        point.setMin(min);
        point.setMax(max);
        point.setStdDev(stdDev);

        MetricsData data = new MetricsData();
        data.setMetrics(Collections.singletonList(point));

        data.getProperties().putAll(properties);
        TelemetryItem telemetry = TelemetryUtil.createTelemetry(data);
        telemetry.getTags().putAll(tags);
        telemetry.setInstrumentationKey(instrumentationKey);

        track(telemetry);
    }

    @Override
    public void trackDependency(String name, String id, String resultCode, @Nullable Long totalMillis,
                                boolean success, String commandName, String type, String target,
                                Map<String, String> properties, Map<String, String> tags, Map<String, Double> metrics,
                                String instrumentationKey) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        RemoteDependencyData data = new RemoteDependencyData();
        data.setName(name);
        data.setId(id);
        data.setResultCode(resultCode);
        if (totalMillis != null) {
            data.setDuration(new Duration(totalMillis));
        }
        data.setSuccess(success);
        data.setData(commandName);
        data.setType(type);
        data.setTarget(target);
        data.getProperties().putAll(properties);
        data.getMeasurements().putAll(metrics);

        TelemetryItem telemetry = TelemetryUtil.createTelemetry(data);
        telemetry.getTags().putAll(tags);
        telemetry.setInstrumentationKey(instrumentationKey);

        track(telemetry);
    }

    @Override
    public void trackPageView(String name, URI uri, long totalMillis, Map<String, String> properties,
                              Map<String, String> tags, Map<String, Double> metrics, String instrumentationKey) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        PageViewData data = new PageViewData();
        data.setName(name);
        data.setUrl(uri);
        data.setDuration(totalMillis);
        data.getProperties().putAll(properties);
        data.getMeasurements().putAll(metrics);

        TelemetryItem telemetry = TelemetryUtil.createTelemetry(data);
        telemetry.getTags().putAll(tags);
        telemetry.setInstrumentationKey(instrumentationKey);

        track(telemetry);
    }

    @Override
    public void trackTrace(String message, int severityLevel, Map<String, String> properties, Map<String, String> tags,
                           String instrumentationKey) {
        if (Strings.isNullOrEmpty(message)) {
            return;
        }

        MessageData data = new MessageData();
        data.setMessage(message);
        if (severityLevel != -1) {
            data.setSeverityLevel(getSeverityLevel(severityLevel));
        }
        data.getProperties().putAll(properties);

        TelemetryItem telemetry = TelemetryUtil.createTelemetry(data);
        telemetry.getTags().putAll(tags);
        telemetry.setInstrumentationKey(instrumentationKey);

        track(telemetry);
    }

    @Override
    public void trackRequest(String id, String name, URL url, Date timestamp, @Nullable Long duration, String responseCode, boolean success,
                             String source, Map<String, String> properties, Map<String, String> tags, Map<String, Double> metrics,
                             String instrumentationKey) {
        if (Strings.isNullOrEmpty(name)) {
            return;
        }

        RequestData data = new RequestData();
        data.setId(id);
        data.setName(name);
        if (url != null) {
            data.setUrl(url);
        }
        if (duration != null) {
            data.setDuration(new Duration(duration));
        }
        data.setResponseCode(responseCode);
        data.setSuccess(success);
        data.setSource(source);
        data.getProperties().putAll(properties);
        data.getMeasurements().putAll(metrics);

        TelemetryItem telemetry = TelemetryUtil.createTelemetry(data);
        telemetry.setTime(timestamp);
        telemetry.getTags().putAll(tags);
        telemetry.setInstrumentationKey(instrumentationKey);

        track(telemetry);
    }

    @Override
    public void trackException(Exception exception, Map<String, String> properties, Map<String, String> tags,
                               Map<String, Double> metrics, String instrumentationKey) {
        if (exception == null) {
            return;
        }

        TelemetryExceptionData data = new TelemetryExceptionData();
        data.setException(exception);
        data.setSeverityLevel(SeverityLevel.Error);
        data.getProperties().putAll(properties);
        data.getMeasurements().putAll(metrics);

        TelemetryItem telemetry = TelemetryUtil.createTelemetry(data);
        telemetry.getTags().putAll(tags);
        telemetry.setInstrumentationKey(instrumentationKey);

        track(telemetry);
    }

    private SeverityLevel getSeverityLevel(int value) {
        for (SeverityLevel sl : SeverityLevel.values()) {
            if (value == sl.getValue()) {
                return sl;
            }
        }
        return null;
    }

    @Override
    public void flush() {
        // this is not null because sdk instrumentation is not added until Global.setTelemetryClient() is called
        checkNotNull(Global.getTelemetryClient()).flush();
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
            telemetry.getTags().put(ContextTagKeys.AI_OPERATION_ID.toString(), context.getTraceId());
            telemetry.getTags().put(ContextTagKeys.AI_OPERATION_PARENT_ID.toString(), context.getSpanId());
            samplingPercentage =
                    Exporter.getSamplingPercentage(context.getTraceState(), Global.getSamplingPercentage(), false);
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
        checkNotNull(Global.getTelemetryClient()).track(telemetry);
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

    // FIXME (trask) share this remaining code with the exporter

    private static final String SAMPLING_PERCENTAGE_TRACE_STATE = "ai-internal-sp";

    private static final Cache<String, OptionalFloat> parsedSamplingPercentageCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .build();

    private static final AtomicBoolean alreadyLoggedSamplingPercentageMissing = new AtomicBoolean();
    private static final AtomicBoolean alreadyLoggedSamplingPercentageParseError = new AtomicBoolean();

    private static float getSamplingPercentage(TraceState traceState, float defaultValue, boolean warnOnMissing) {
        String samplingPercentageStr = traceState.get(SAMPLING_PERCENTAGE_TRACE_STATE);
        if (samplingPercentageStr == null) {
            if (warnOnMissing && !alreadyLoggedSamplingPercentageMissing.getAndSet(true)) {
                // sampler should have set the trace state
                logger.warn("did not find sampling percentage in trace state: {}", traceState);
            }
            return defaultValue;
        }
        try {
            return parseSamplingPercentage(samplingPercentageStr).orElse(defaultValue);
        } catch (ExecutionException e) {
            // this shouldn't happen
            logger.debug(e.getMessage(), e);
            return defaultValue;
        }
    }

    private static OptionalFloat parseSamplingPercentage(String samplingPercentageStr) throws ExecutionException {
        return parsedSamplingPercentageCache.get(samplingPercentageStr, () -> {
            try {
                return OptionalFloat.of(Float.parseFloat(samplingPercentageStr));
            } catch (NumberFormatException e) {
                if (!alreadyLoggedSamplingPercentageParseError.getAndSet(true)) {
                    logger.warn("error parsing sampling percentage trace state: {}", samplingPercentageStr, e);
                }
                return OptionalFloat.empty();
            }
        });
    }

    private static class OptionalFloat {

        private static final OptionalFloat EMPTY = new OptionalFloat();

        private final boolean present;
        private final float value;

        private OptionalFloat() {
            this.present = false;
            this.value = Float.NaN;
        }

        private OptionalFloat(float value) {
            this.present = true;
            this.value = value;
        }

        public static OptionalFloat empty() {
            return EMPTY;
        }

        public static OptionalFloat of(float value) {
            return new OptionalFloat(value);
        }

        public float orElse(float other) {
            return present ? value : other;
        }

        public boolean isEmpty() {
            return !present;
        }
    }
}
