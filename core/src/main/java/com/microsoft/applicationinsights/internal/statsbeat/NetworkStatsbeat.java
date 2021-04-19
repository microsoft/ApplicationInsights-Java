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
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_INSTRUMENTATION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.DEFAULT_STATSBEAT_INTERVAL;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.EXCEPTION_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_DURATION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_FAILURE_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_SUCCESS_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RETRY_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.THROTTLE_COUNT;

public class NetworkStatsbeat extends BaseStatsbeat {

    private static final Logger logger = LoggerFactory.getLogger(NetworkStatsbeat.class);
    private static volatile Set<String> instrumentationList;

    public NetworkStatsbeat() {
        super();
        instrumentationList = new HashSet<>(64);
    }

    @Override
    protected void send(TelemetryClient telemetryClient) {
        String instrumentation = String.valueOf(getInstrumentation());
        
        StatsbeatTelemetry requestSuccessCountSt = createStatsbeatTelemetry(REQUEST_SUCCESS_COUNT, requestSuccessCount);
        requestSuccessCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
        telemetryClient.track(requestSuccessCountSt);
        logger.debug("#### sending {}: {}", REQUEST_SUCCESS_COUNT, requestSuccessCountSt);

        StatsbeatTelemetry requestFailureCountSt = createStatsbeatTelemetry(REQUEST_FAILURE_COUNT, requestFailureCount);
        requestFailureCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
        telemetryClient.track(requestFailureCountSt);
        logger.debug("#### sending {}: {}", REQUEST_FAILURE_COUNT, requestFailureCountSt);

        double durationAvg = getRequestDurationAvg();
        StatsbeatTelemetry requestFailureDurationSt = createStatsbeatTelemetry(REQUEST_DURATION, durationAvg);
        requestFailureDurationSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
        telemetryClient.track(requestFailureDurationSt);
        logger.debug("#### sending {}: {}", REQUEST_DURATION, durationAvg);

        StatsbeatTelemetry retryCountSt = createStatsbeatTelemetry(RETRY_COUNT, retryCount);
        retryCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
        telemetryClient.track(retryCountSt);
        logger.debug("#### sending {}: {}", RETRY_COUNT, retryCount);

        StatsbeatTelemetry throttleCountSt = createStatsbeatTelemetry(THROTTLE_COUNT, throttleCount);
        throttleCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
        telemetryClient.track(throttleCountSt);
        logger.debug("#### sending {}: {}", THROTTLE_COUNT, throttleCount);

        StatsbeatTelemetry exceptionCountSt = createStatsbeatTelemetry(EXCEPTION_COUNT, exceptionCount);
        exceptionCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
        telemetryClient.track(exceptionCountSt);
        logger.debug("#### sending {}: {}", EXCEPTION_COUNT, exceptionCount);
    }

    @Override
    protected void reset() {
        instrumentationList = new HashSet<>(64);
        requestSuccessCount = 0;
        requestFailureCount = 0;
        requestDurations = new ArrayList<>();
        retryCount = 0;
        throttleCount = 0;
        exceptionCount = 0;
    }

    public static void addInstrumentation(String instrumentation) {
        instrumentationList.add(instrumentation);
        logger.debug("#### add {} to the list", instrumentation);
        logger.debug("#### instrumentation list size: {}", instrumentationList.size());
    }

    /**
     * @return a 64-bit long that represents a list of instrumentations enabled. Each bitfield maps to an instrumentation.
     */
    public long getInstrumentation() {
        return StatsbeatHelper.encodeInstrumentations(instrumentationList);
    }

    private volatile long requestSuccessCount;
    private volatile long requestFailureCount;
    private volatile List<Double> requestDurations = new ArrayList<>();
    private volatile long retryCount;
    private volatile long throttleCount;
    private volatile long exceptionCount;

    public void incrementRequestSuccessCount() {
        requestSuccessCount++;
    }

    public void incrementRequestFailureCount() {
        requestFailureCount++;
    }

    public void addRequestDuration(double duration) {
        requestDurations.add(duration);
    }

    public void incrementRetryCount() {
        retryCount++;
    }

    public void incrementThrottleCount() {
        throttleCount++;
    }

    public void incrementExceptionCount() {
        exceptionCount++;
    }

    private double getRequestDurationAvg() {
        double sum = 0L;
        for (double elem : requestDurations) {
            sum += elem;
        }

        return sum/requestDurations.size();
    }
}
