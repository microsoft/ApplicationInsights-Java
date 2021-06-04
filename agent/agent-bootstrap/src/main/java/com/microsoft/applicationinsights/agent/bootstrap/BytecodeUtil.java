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
package com.microsoft.applicationinsights.agent.bootstrap;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import io.opentelemetry.instrumentation.api.aisdk.MicrometerUtil;
import io.opentelemetry.instrumentation.api.aisdk.MicrometerUtil.MicrometerUtilDelegate;

import static java.util.concurrent.TimeUnit.*;

// supporting all properties of event, metric, remote dependency and page view telemetry
public class BytecodeUtil {

    private static BytecodeUtilDelegate delegate;

    public static void setDelegate(final BytecodeUtilDelegate delegate) {
        if (BytecodeUtil.delegate == null) {
            BytecodeUtil.delegate = delegate;
            MicrometerUtil.setDelegate(new MicrometerUtilDelegate() {
                @Override
                public void trackMetric(String name, double value, Integer count, Double min, Double max, Map<String, String> properties) {
                    delegate.trackMetric(null, name, value, count, min, max, null, properties, Collections.emptyMap(), null);
                }
            });
        }
    }

    public static void trackEvent(Date timestamp, String name, Map<String, String> properties, Map<String, String> tags,
                                  Map<String, Double> metrics, String instrumentationKey) {
        if (delegate != null) {
            delegate.trackEvent(timestamp, name, properties, tags, metrics, instrumentationKey);
        }
    }

    public static void trackMetric(Date timestamp, String name, double value, Integer count, Double min, Double max, Double stdDev,
                                   Map<String, String> properties, Map<String, String> tags, String instrumentationKey) {
        if (delegate != null) {
            delegate.trackMetric(timestamp, name, value, count, min, max, stdDev, properties, tags, instrumentationKey);
        }
    }

    public static void trackDependency(Date timestamp, String name, String id, String resultCode, Long totalMillis, boolean success,
                                       String commandName, String type, String target, Map<String, String> properties,
                                       Map<String, String> tags, Map<String, Double> metrics, String instrumentationKey) {
        if (delegate != null) {
            delegate.trackDependency(timestamp, name, id, resultCode, totalMillis, success, commandName, type, target, properties,
                    tags, metrics, instrumentationKey);
        }
    }

    public static void trackPageView(Date timestamp, String name, URI uri, long totalMillis, Map<String, String> properties, Map<String, String> tags,
                                     Map<String, Double> metrics, String instrumentationKey) {
        if (delegate != null) {
            delegate.trackPageView(timestamp, name, uri, totalMillis, properties, tags, metrics, instrumentationKey);
        }
    }

    public static void trackTrace(Date timestamp, String message, int severityLevel, Map<String, String> properties, Map<String, String> tags,
                                  String instrumentationKey) {
        if (delegate != null) {
            delegate.trackTrace(timestamp, message, severityLevel, properties, tags, instrumentationKey);
        }
    }

    public static void trackRequest(String id, String name, URL url, Date timestamp, Long duration, String responseCode, boolean success,
                                    String source, Map<String, String> properties, Map<String, String> tags, Map<String, Double> metrics,
                                    String instrumentationKey) {
        if (delegate != null) {
            delegate.trackRequest(id, name, url, timestamp, duration, responseCode, success, source, properties, tags,
                    metrics, instrumentationKey);
        }
    }

    public static void trackException(Date timestamp, Exception exception, Map<String, String> properties, Map<String, String> tags,
                                      Map<String, Double> metrics, String instrumentationKey) {
        if (delegate != null) {
            delegate.trackException(timestamp, exception, properties, tags, metrics, instrumentationKey);
        }
    }

    public static void flush() {
        if (delegate != null) {
            delegate.flush();
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

    // originally from SDK MapUtil.copy()
    public static void copy(Map<String, String> source, Map<String, String> target, String excludePrefix) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            if (excludePrefix != null && key.startsWith(excludePrefix)) {
                continue;
            }
            if (!target.containsKey(key)) {
                String value = entry.getValue();
                if (value != null) {
                    target.put(key, value);
                }
            }
        }
    }

    // this exists only to support -Dapplicationinsights.debug.signedJarAccess=true
    private static final ThreadLocal<Long> startNanosHolder = new ThreadLocal<>();

    public static void onEnter() {
        startNanosHolder.set(System.nanoTime());
    }

    public static void onExit() {
        Long startNanos = startNanosHolder.get();
        if (startNanos == null) {
            System.out.println("Signed jar access (no timing available)");
            Thread.dumpStack();
        } else {
            long durationNanos = System.nanoTime() - startNanos;
            if (durationNanos > MILLISECONDS.toNanos(1)) {
                System.out.println("Signed jar access (" + NANOSECONDS.toMillis(durationNanos) + " milliseconds)");
                Thread.dumpStack();
            }
        }
        startNanosHolder.remove();
    }

    public interface BytecodeUtilDelegate {

        void trackEvent(Date timestamp, String name, Map<String, String> properties, Map<String, String> tags, Map<String, Double> metrics,
                        String instrumentationKey);

        void trackMetric(Date timestamp, String name, double value, Integer count, Double min, Double max,
                         Double stdDev, Map<String, String> properties, Map<String, String> tags,
                         String instrumentationKey);

        void trackDependency(Date timestamp, String name, String id, String resultCode, Long totalMillis,
                             boolean success, String commandName, String type, String target,
                             Map<String, String> properties, Map<String, String> tags, Map<String, Double> metrics,
                             String instrumentationKey);

        void trackPageView(Date timestamp, String name, URI uri, long totalMillis, Map<String, String> properties, Map<String, String> tags,
                           Map<String, Double> metrics, String instrumentationKey);

        void trackTrace(Date timestamp, String message, int severityLevel, Map<String, String> properties, Map<String, String> tags,
                        String instrumentationKey);

        void trackRequest(String id, String name, URL url, Date timestamp, Long duration, String responseCode, boolean success,
                          String source, Map<String, String> properties, Map<String, String> tags, Map<String, Double> metrics,
                          String instrumentationKey);

        // TODO also handle cases where ExceptionTelemetry parsedStack is used directly instead of indirectly through Exception
        void trackException(Date timestamp, Exception exception, Map<String, String> properties, Map<String, String> tags,
                            Map<String, Double> metrics, String instrumentationKey);

        void flush();

        void logErrorOnce(Throwable t);
    }
}
