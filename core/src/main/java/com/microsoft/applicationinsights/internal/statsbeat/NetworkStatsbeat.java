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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_INSTRUMENTATION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.EXCEPTION_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_DURATION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_FAILURE_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_SUCCESS_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RETRY_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.THROTTLE_COUNT;

public class NetworkStatsbeat extends BaseStatsbeat {

    private static final Logger logger = LoggerFactory.getLogger(NetworkStatsbeat.class);
    private static volatile Set<String> instrumentationList = new HashSet<>(64);;

    public NetworkStatsbeat(TelemetryClient telemetryClient) {
        super(telemetryClient);
    }

    @Override
    protected void send() {
        String instrumentation = String.valueOf(getInstrumentation());

        if (requestSuccessCount != 0) {
            StatsbeatTelemetry requestSuccessCountSt = createStatsbeatTelemetry(REQUEST_SUCCESS_COUNT, requestSuccessCount);
            requestSuccessCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(requestSuccessCountSt);
            logger.debug("#### send a NetworkStatsbeat {}: {}", REQUEST_SUCCESS_COUNT, requestSuccessCountSt);
        }

        if (requestFailureCount != 0) {
            StatsbeatTelemetry requestFailureCountSt = createStatsbeatTelemetry(REQUEST_FAILURE_COUNT, requestFailureCount);
            requestFailureCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(requestFailureCountSt);
            logger.debug("#### send a NetworkStatsbeat {}: {}", REQUEST_FAILURE_COUNT, requestFailureCountSt);
        }

        double durationAvg = getRequestDurationAvg();
        if (durationAvg != 0) {
            StatsbeatTelemetry requestDurationSt = createStatsbeatTelemetry(REQUEST_DURATION, durationAvg);
            requestDurationSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(requestDurationSt);
            logger.debug("#### send a NetworkStatsbeat {}: {}", REQUEST_DURATION, requestDurationSt);
        }

        if (retryCount != 0) {
            StatsbeatTelemetry retryCountSt = createStatsbeatTelemetry(RETRY_COUNT, retryCount);
            retryCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(retryCountSt);
            logger.debug("#### send a NetworkStatsbeat {}: {}", RETRY_COUNT, retryCountSt);
        }

        if (throttlingCount != 0) {
            StatsbeatTelemetry throttleCountSt = createStatsbeatTelemetry(THROTTLE_COUNT, throttlingCount);
            throttleCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(throttleCountSt);
            logger.debug("#### send a NetworkStatsbeat {}: {}", THROTTLE_COUNT, throttleCountSt);
        }

        if (exceptionCount != 0) {
            StatsbeatTelemetry exceptionCountSt = createStatsbeatTelemetry(EXCEPTION_COUNT, exceptionCount);
            exceptionCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(exceptionCountSt);
            logger.debug("#### send a NetworkStatsbeat{}: {}", EXCEPTION_COUNT, exceptionCountSt);
        }
    }

    @Override
    protected void reset() {
        instrumentationList = new HashSet<>(64);
        requestSuccessCount = 0;
        requestFailureCount = 0;
        requestDurations = new ArrayList<>();
        retryCount = 0;
        throttlingCount = 0;
        exceptionCount = 0;
        logger.debug("#### reset NetworkStatsbeat");
    }

    public void addInstrumentation(String instrumentation) {
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

    private static volatile long requestSuccessCount;
    private static volatile long requestFailureCount;
    private static volatile List<Double> requestDurations = new ArrayList<>();
    private static volatile long retryCount;
    private static long throttlingCount;
    private static long exceptionCount;

    public void incrementRequestSuccessCount() {
        requestSuccessCount++;
        logger.debug("#### increment request success count");
    }

    public void incrementRequestFailureCount() {
        requestFailureCount++;
        logger.debug("#### increment request failure count");
    }

    public void addRequestDuration(double duration) {
        requestDurations.add(duration);
        logger.debug("#### add a new request duration {}", duration);
    }

    public void incrementRetryCount() {
        retryCount++;
        logger.debug("#### increment retry count");
    }

    public void incrementThrottlingCount() {
        throttlingCount++;
    }

    public void incrementExceptionCount() {
        exceptionCount++;
    }

    public long getRequestSuccessCount() {
        return requestSuccessCount;
    }

    public long getRequestFailureCount() {
        return requestFailureCount;
    }

    public List<Double> getRequestDurations() {
        return requestDurations;
    }

    public long getRetryCount() {
        return retryCount;
    }

    public long getThrottlingCount() {
        return throttlingCount;
    }

    public long getExceptionCount() {
        return exceptionCount;
    }

    protected double getRequestDurationAvg() {
        double sum = 0.0;
        for (double elem : requestDurations) {
            sum += elem;
        }

        if (requestDurations.size() != 0) {
            return sum / requestDurations.size();
        }

        return  sum;
    }
}
