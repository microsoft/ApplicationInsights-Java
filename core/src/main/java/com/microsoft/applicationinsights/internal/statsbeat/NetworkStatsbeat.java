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

package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_INSTRUMENTATION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.EXCEPTION_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_DURATION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_FAILURE_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_SUCCESS_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RETRY_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.THROTTLE_COUNT;

public class NetworkStatsbeat extends BaseStatsbeat {

    private volatile IntervalMetrics current;
    private final Object lock = new Object();

    public NetworkStatsbeat(TelemetryClient telemetryClient, long interval) {
        super(telemetryClient, interval);
        current = new IntervalMetrics();
    }

    @Override
    protected void send() {
        IntervalMetrics local = current;
        current = new IntervalMetrics();

        String instrumentation = String.valueOf(getInstrumentation());

        if (local.requestSuccessCount.get() != 0) {
            MetricTelemetry requestSuccessCountSt = createStatsbeatTelemetry(REQUEST_SUCCESS_COUNT, local.requestSuccessCount.get());
            requestSuccessCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(requestSuccessCountSt);
        }

        if (local.requestFailureCount.get() != 0) {
            MetricTelemetry requestFailureCountSt = createStatsbeatTelemetry(REQUEST_FAILURE_COUNT, local.requestFailureCount.get());
            requestFailureCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(requestFailureCountSt);
        }

        double durationAvg = getRequestDurationAvg();
        if (durationAvg != 0) {
            MetricTelemetry requestDurationSt = createStatsbeatTelemetry(REQUEST_DURATION, durationAvg);
            requestDurationSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(requestDurationSt);
        }

        if (local.retryCount.get() != 0) {
            MetricTelemetry retryCountSt = createStatsbeatTelemetry(RETRY_COUNT, local.retryCount.get());
            retryCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(retryCountSt);
        }

        if (local.throttlingCount.get() != 0) {
            MetricTelemetry throttleCountSt = createStatsbeatTelemetry(THROTTLE_COUNT, local.throttlingCount.get());
            throttleCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(throttleCountSt);
        }

        if (local.exceptionCount.get() != 0) {
            MetricTelemetry exceptionCountSt = createStatsbeatTelemetry(EXCEPTION_COUNT, local.exceptionCount.get());
            exceptionCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(exceptionCountSt);
        }
    }

    // TODO create a new instrumentation set automatically when its capacity reaches 64
    public void addInstrumentation(String instrumentation) {
        synchronized (lock) {
            current.instrumentationList.add(instrumentation);
        }
    }

    public Set<String> getInstrumentationList() {
        return current.instrumentationList;
    }

    /**
     * @return a 64-bit long that represents a list of instrumentations enabled. Each bitfield maps to an instrumentation.
     */
    public long getInstrumentation() {
        return StatsbeatHelper.encodeInstrumentations(current.instrumentationList);
    }

    public void incrementRequestSuccessCount() {
        current.requestSuccessCount.incrementAndGet();
    }

    public void incrementRequestFailureCount() {
        current.requestFailureCount.incrementAndGet();
    }

    public void addRequestDuration(double duration) {
        synchronized (lock) {
            current.requestDurations.add(duration);
        }
    }

    public void incrementRetryCount() {
        current.retryCount.incrementAndGet();
    }

    public void incrementThrottlingCount() {
        current.throttlingCount.incrementAndGet();
    }

    public void incrementExceptionCount() {
        current.exceptionCount.incrementAndGet();
    }

    public long getRequestSuccessCount() {
        return current.requestSuccessCount.get();
    }

    public long getRequestFailureCount() {
        return current.requestFailureCount.get();
    }

    public List<Double> getRequestDurations() {
        return current.requestDurations;
    }

    public long getRetryCount() {
        return current.retryCount.get();
    }

    public long getThrottlingCount() {
        return current.throttlingCount.get();
    }

    public long getExceptionCount() {
        return current.exceptionCount.get();
    }

    protected double getRequestDurationAvg() {
        double sum = 0.0;
        for (double elem : current.requestDurations) {
            sum += elem;
        }

        if (current.requestDurations.size() != 0) {
            return sum / current.requestDurations.size();
        }

        return  sum;
    }

    private static class IntervalMetrics {
        private volatile Set<String> instrumentationList = new HashSet<>(64);
        private final AtomicLong requestSuccessCount = new AtomicLong(0);
        private final AtomicLong requestFailureCount = new AtomicLong(0);
        private volatile List<Double> requestDurations = new ArrayList<>();
        private final AtomicLong retryCount = new AtomicLong(0);
        private final AtomicLong throttlingCount = new AtomicLong(0);
        private final AtomicLong exceptionCount = new AtomicLong(0);
    }
}
