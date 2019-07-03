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
package com.microsoft.applicationinsights.agentc.internal.model;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.util.MapUtil;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

// supporting all properties of event, metric, remove dependency and page view telemetry
public class LegacySDK {

    private static final Logger logger = LoggerFactory.getLogger(LegacySDK.class);

    private static final AtomicBoolean alreadyLoggedError = new AtomicBoolean();

    public static void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        EventTelemetry telemetry = new EventTelemetry(name);
        MapUtil.copy(properties, telemetry.getContext().getProperties());
        MapUtil.copy(metrics, telemetry.getMetrics());

        track(telemetry);
    }

    // TODO do not track if perf counter (?)
    public static void trackMetric(String name, double value, Integer count, Double min, Double max,
                                   Double stdDev, Map<String, String> properties) {

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
        MapUtil.copy(properties, telemetry.getProperties());

        track(telemetry);
    }

    public static void trackDependency(String name, String id, String resultCode, @Nullable Long totalMillis,
                                       boolean success, String commandName, String type, String target,
                                       Map<String, String> properties, Map<String, Double> metrics) {

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
        MapUtil.copy(properties, telemetry.getProperties());
        MapUtil.copy(metrics, telemetry.getMetrics());

        track(telemetry);
    }

    public static void trackPageView(String name, URI uri, long totalMillis, Map<String, String> properties,
                                     Map<String, Double> metrics) {

        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        PageViewTelemetry telemetry = new PageViewTelemetry();
        telemetry.setName(name);
        telemetry.setUrl(uri);
        telemetry.setDuration(totalMillis);
        MapUtil.copy(properties, telemetry.getProperties());
        MapUtil.copy(metrics, telemetry.getMetrics());

        track(telemetry);
    }

    public static long getTotalMilliseconds(long days, int hours, int minutes, int seconds, int milliseconds) {
        return DAYS.toMillis(days)
                + HOURS.toMillis(hours)
                + MINUTES.toMillis(minutes)
                + SECONDS.toMillis(seconds)
                + milliseconds;
    }

    public static void logErrorOnce(Throwable t) {
        if (!alreadyLoggedError.getAndSet(true)) {
            logger.error(t.getMessage(), t);
        }
    }

    private static void track(Telemetry telemetry) {
        ThreadContextPlus threadContext = Global.getThreadContextThreadLocal().get();
        if (threadContext instanceof ThreadContextImpl) {
            IncomingSpanImpl incomingSpan = ((ThreadContextImpl) threadContext).getIncomingSpan();
            telemetry.getContext().getOperation().setId(incomingSpan.getOperationId());
            telemetry.getContext().getOperation().setParentId(incomingSpan.getOperationParentId());
        }
        Global.getTelemetryClient().track(telemetry);
    }
}
