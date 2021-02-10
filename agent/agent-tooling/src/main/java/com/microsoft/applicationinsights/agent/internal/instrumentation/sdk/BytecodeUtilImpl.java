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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.BytecodeUtilDelegate;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.sampling.SamplingScoreGeneratorV2;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.SupportSampling;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
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
    public void trackEvent(String name, Map<String, String> properties, Map<String, String> tags, Map<String, Double> metrics) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        EventTelemetry telemetry = new EventTelemetry(name);
        telemetry.getProperties().putAll(properties);
        telemetry.getContext().getTags().putAll(tags);
        telemetry.getMetrics().putAll(metrics);

        track(telemetry);
    }

    // TODO do not track if perf counter (?)
    @Override
    public void trackMetric(String name, double value, Integer count, Double min, Double max,
                            Double stdDev, Map<String, String> properties, Map<String, String> tags) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        MetricTelemetry telemetry = new MetricTelemetry();
        telemetry.setName(name);
        telemetry.setValue(value);
        telemetry.setCount(count);
        telemetry.setMin(min);
        telemetry.setMax(max);
        telemetry.setStandardDeviation(stdDev);
        telemetry.getProperties().putAll(properties);
        telemetry.getContext().getTags().putAll(tags);

        track(telemetry);
    }

    @Override
    public void trackDependency(String name, String id, String resultCode, @Nullable Long totalMillis,
                                boolean success, String commandName, String type, String target,
                                Map<String, String> properties, Map<String, String> tags, Map<String, Double> metrics) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();
        telemetry.setName(name);
        telemetry.setId(id);
        telemetry.setResultCode(resultCode);
        if (totalMillis != null) {
            telemetry.setDuration(new Duration(totalMillis));
        }
        telemetry.setSuccess(success);
        telemetry.setCommandName(commandName);
        telemetry.setType(type);
        telemetry.setTarget(target);
        telemetry.getProperties().putAll(properties);
        telemetry.getContext().getTags().putAll(tags);
        telemetry.getMetrics().putAll(metrics);

        track(telemetry);
    }

    @Override
    public void trackPageView(String name, URI uri, long totalMillis, Map<String, String> properties,
                              Map<String, String> tags, Map<String, Double> metrics) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        PageViewTelemetry telemetry = new PageViewTelemetry();
        telemetry.setName(name);
        telemetry.setUrl(uri);
        telemetry.setDuration(totalMillis);
        telemetry.getProperties().putAll(properties);
        telemetry.getContext().getTags().putAll(tags);
        telemetry.getMetrics().putAll(metrics);

        track(telemetry);
    }

    @Override
    public void trackTrace(String message, int severityLevel, Map<String, String> properties, Map<String, String> tags) {
        if (Strings.isNullOrEmpty(message)) {
            return;
        }

        TraceTelemetry telemetry = new TraceTelemetry();
        telemetry.setMessage(message);
        if (severityLevel != -1) {
            telemetry.setSeverityLevel(getSeverityLevel(severityLevel));
        }
        telemetry.getProperties().putAll(properties);
        telemetry.getContext().getTags().putAll(tags);

        track(telemetry);
    }

    @Override
    public void trackRequest(String id, String name, URL url, Date timestamp, @Nullable Long duration, String responseCode, boolean success,
                             String source, Map<String, String> properties, Map<String, String> tags) {
        if (Strings.isNullOrEmpty(name)) {
            return;
        }

        RequestTelemetry telemetry = new RequestTelemetry();
        telemetry.setId(id);
        telemetry.setName(name);
        if (url != null) {
            telemetry.setUrl(url);
        }
        telemetry.setTimestamp(timestamp);
        if (duration != null) {
            telemetry.setDuration(new Duration(duration));
        }
        telemetry.setResponseCode(responseCode);
        telemetry.setSuccess(success);
        telemetry.setSource(source);
        telemetry.getProperties().putAll(properties);
        telemetry.getContext().getTags().putAll(tags);

        track(telemetry);
    }

    @Override
    public void trackException(Exception exception, Map<String, String> properties, Map<String, String> tags,
                               Map<String, Double> metrics) {
        if (exception == null) {
            return;
        }

        ExceptionTelemetry telemetry = new ExceptionTelemetry();
        telemetry.setException(exception);
        telemetry.setSeverityLevel(SeverityLevel.Error);
        telemetry.getProperties().putAll(properties);
        telemetry.getContext().getTags().putAll(tags);
        telemetry.getMetrics().putAll(metrics);

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

    private static void track(Telemetry telemetry) {
        SpanContext context = Span.current().getSpanContext();
        if (context.isValid()) {
            String traceId = context.getTraceId();
            String spanId = context.getSpanId();
            telemetry.getContext().getOperation().setId(traceId);
            telemetry.getContext().getOperation().setParentId(spanId);
        }
        double samplingPercentage = Global.getSamplingPercentage();
        if (sample(telemetry, samplingPercentage)) {
            if (telemetry instanceof SupportSampling && samplingPercentage != 100) {
                ((SupportSampling) telemetry).setSamplingPercentage(samplingPercentage);
            }
            // this is not null because sdk instrumentation is not added until Global.setTelemetryClient() is called
            checkNotNull(Global.getTelemetryClient()).track(telemetry);
        }
    }

    private static boolean sample(Telemetry telemetry, double samplingPercentage) {
        if (samplingPercentage == 100) {
            return true;
        }
        if (SamplingScoreGeneratorV2.getSamplingScore(telemetry.getContext().getOperation().getId()) >=
                samplingPercentage) {
            logger.debug("Item {} sampled out", telemetry.getClass().getSimpleName());
            return false;
        }
        return true;
    }
}
