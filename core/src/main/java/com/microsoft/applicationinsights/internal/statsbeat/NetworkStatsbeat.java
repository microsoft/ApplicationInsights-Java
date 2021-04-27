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
import java.util.concurrent.atomic.AtomicLong;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.CUSTOM_DIMENSIONS_INSTRUMENTATION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.EXCEPTION_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_DURATION;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_FAILURE_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.REQUEST_SUCCESS_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.RETRY_COUNT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.THROTTLE_COUNT;

public class NetworkStatsbeat extends BaseStatsbeat {

    private static final Logger logger = LoggerFactory.getLogger(NetworkStatsbeat.class);
    private static volatile Set<String> instrumentationList = new HashSet<>(64);
    private static final AtomicLong requestSuccessCount = new AtomicLong(0);
    private static final AtomicLong requestFailureCount = new AtomicLong(0);
    private static volatile List<Double> requestDurations = new ArrayList<>();
    private static final AtomicLong retryCount = new AtomicLong(0);
    private static final AtomicLong throttlingCount = new AtomicLong(0);
    private static final AtomicLong exceptionCount = new AtomicLong(0);
    private final Object lock = new Object();

    public NetworkStatsbeat(TelemetryClient telemetryClient) {
        super(telemetryClient);
    }

    @Override
    protected void send() {
        String instrumentation = String.valueOf(getInstrumentation());

        if (requestSuccessCount.get() != 0) {
            StatsbeatTelemetry requestSuccessCountSt = createStatsbeatTelemetry(REQUEST_SUCCESS_COUNT, requestSuccessCount.get());
            requestSuccessCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(requestSuccessCountSt);
            logger.debug("#### send a NetworkStatsbeat {}: {}", REQUEST_SUCCESS_COUNT, requestSuccessCountSt);
        }

        if (requestFailureCount.get() != 0) {
            StatsbeatTelemetry requestFailureCountSt = createStatsbeatTelemetry(REQUEST_FAILURE_COUNT, requestFailureCount.get());
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

        if (retryCount.get() != 0) {
            StatsbeatTelemetry retryCountSt = createStatsbeatTelemetry(RETRY_COUNT, retryCount.get());
            retryCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(retryCountSt);
            logger.debug("#### send a NetworkStatsbeat {}: {}", RETRY_COUNT, retryCountSt);
        }

        if (throttlingCount.get() != 0) {
            StatsbeatTelemetry throttleCountSt = createStatsbeatTelemetry(THROTTLE_COUNT, throttlingCount.get());
            throttleCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(throttleCountSt);
            logger.debug("#### send a NetworkStatsbeat {}: {}", THROTTLE_COUNT, throttleCountSt);
        }

        if (exceptionCount.get() != 0) {
            StatsbeatTelemetry exceptionCountSt = createStatsbeatTelemetry(EXCEPTION_COUNT, exceptionCount.get());
            exceptionCountSt.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, instrumentation);
            telemetryClient.track(exceptionCountSt);
            logger.debug("#### send a NetworkStatsbeat{}: {}", EXCEPTION_COUNT, exceptionCountSt);
        }
    }

    @Override
    protected void reset() {
        instrumentationList = new HashSet<>(64);
        requestSuccessCount.set(0L);
        requestFailureCount.set(0L);
        requestDurations = new ArrayList<>();
        retryCount.set(0L);
        throttlingCount.set(0L);
        exceptionCount.set(0L);
        logger.debug("#### reset NetworkStatsbeat");
    }

    public void addInstrumentation(String instrumentation) {
        synchronized (lock) {
            instrumentationList.add(instrumentation);
            logger.debug("#### add {} to the list", instrumentation);
            logger.debug("#### instrumentation list size: {}", instrumentationList.size());
        }
    }

    /**
     * @return a 64-bit long that represents a list of instrumentations enabled. Each bitfield maps to an instrumentation.
     */
    public long getInstrumentation() {
        return StatsbeatHelper.encodeInstrumentations(instrumentationList);
    }

    public void incrementRequestSuccessCount() {
        logger.debug("#### requestSuccessCount={}", requestSuccessCount.incrementAndGet());
        logger.debug("#### increment request success count");
    }

    public void incrementRequestFailureCount() {
        logger.debug("#### requestFailureCount={}", requestFailureCount.incrementAndGet());
        logger.debug("#### increment request failure count");
    }

    public void addRequestDuration(double duration) {
        synchronized (lock) {
            requestDurations.add(duration);
            logger.debug("#### add a new request duration {}", requestDurations.size());
        }
    }

    public void incrementRetryCount() {
        logger.debug("#### retryCount={}", retryCount.incrementAndGet());
        logger.debug("#### increment retry count");
    }

    public void incrementThrottlingCount() {
        logger.debug("#### throttlingCount={}", throttlingCount.incrementAndGet());
        logger.debug("#### increment throttling count");
    }

    public void incrementExceptionCount() {
        logger.debug("#### exceptionCount={}", exceptionCount.incrementAndGet());
        logger.debug("#### increment exception count");
    }

    public long getRequestSuccessCount() {
        return requestSuccessCount.get();
    }

    public long getRequestFailureCount() {
        return requestFailureCount.get();
    }

    public List<Double> getRequestDurations() {
        return requestDurations;
    }

    public long getRetryCount() {
        return retryCount.get();
    }

    public long getThrottlingCount() {
        return throttlingCount.get();
    }

    public long getExceptionCount() {
        return exceptionCount.get();
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
