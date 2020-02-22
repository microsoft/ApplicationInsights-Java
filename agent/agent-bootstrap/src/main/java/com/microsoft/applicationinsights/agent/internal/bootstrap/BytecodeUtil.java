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
package com.microsoft.applicationinsights.agent.internal.bootstrap;

import java.net.URI;
import java.util.Map;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

// supporting all properties of event, metric, remote dependency and page view telemetry
public class BytecodeUtil {

    private static BytecodeUtilDelegate delegate;

    public static void setDelegate(BytecodeUtilDelegate delegate) {
        if (BytecodeUtil.delegate == null) {
            BytecodeUtil.delegate = delegate;
        }
    }

    public static void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics) {
        if (delegate != null) {
            delegate.trackEvent(name, properties, metrics);
        }
    }

    public static void trackMetric(String name, double value, Integer count, Double min, Double max, Double stdDev,
                                   Map<String, String> properties) {
        if (delegate != null) {
            delegate.trackMetric(name, value, count, min, max, stdDev, properties);
        }
    }

    public static void trackDependency(String name, String id, String resultCode, Long totalMillis, boolean success,
                                       String commandName, String type, String target, Map<String, String> properties,
                                       Map<String, Double> metrics) {
        if (delegate != null) {
            delegate.trackDependency(name, id, resultCode, totalMillis, success, commandName, type, target, properties,
                    metrics);
        }
    }

    public static void trackPageView(String name, URI uri, long totalMillis, Map<String, String> properties,
                                     Map<String, Double> metrics) {
        if (delegate != null) {
            delegate.trackPageView(name, uri, totalMillis, properties, metrics);
        }
    }

    public static void logErrorOnce(Throwable t) {
        if (delegate != null) {
            delegate.logErrorOnce(t);
        }
    }

    public static long getTotalMilliseconds(long days, int hours, int minutes, int seconds, int milliseconds) {
        return DAYS.toMillis(days)
                + HOURS.toMillis(hours)
                + MINUTES.toMillis(minutes)
                + SECONDS.toMillis(seconds)
                + milliseconds;
    }

    public interface BytecodeUtilDelegate {

        void trackEvent(String name, Map<String, String> properties, Map<String, Double> metrics);

        void trackMetric(String name, double value, Integer count, Double min, Double max,
                         Double stdDev, Map<String, String> properties);

        void trackDependency(String name, String id, String resultCode, Long totalMillis,
                             boolean success, String commandName, String type, String target,
                             Map<String, String> properties, Map<String, Double> metrics);

        void trackPageView(String name, URI uri, long totalMillis, Map<String, String> properties,
                           Map<String, Double> metrics);

        void logErrorOnce(Throwable t);
    }
}
