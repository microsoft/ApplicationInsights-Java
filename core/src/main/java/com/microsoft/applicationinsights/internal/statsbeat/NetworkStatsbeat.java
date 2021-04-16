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

public class NetworkStatsbeat extends BaseStatsbeat {

    private static final Logger logger = LoggerFactory.getLogger(NetworkStatsbeat.class);
    private static volatile Set<String> instrumentationList;

    public NetworkStatsbeat() {
        super();
        instrumentationList = new HashSet<>(64);
    }

    @Override
    public void send(TelemetryClient telemetryClient) {
        StatsbeatTelemetry statsbeatTelemetry = createStatsbeatTelemetry(NetworkStatsbeat.class.getName());
        statsbeatTelemetry.getProperties().put(CUSTOM_DIMENSIONS_INSTRUMENTATION, String.valueOf(getInstrumentation()));
        telemetryClient.track(statsbeatTelemetry);
        logger.debug("#### sending AdvancedStatsbeat");
        reset();
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
    private volatile List<Long> requestDurations = new ArrayList<>();
    private volatile long retryCount;
    private volatile long throttleCount;
    private volatile long exceptionCount;

    public void incrementRequestSuccessCount() {
        requestSuccessCount++;
    }

    public void incrementRequestFailureCount() {
        requestFailureCount++;
    }

    public void addRequestDuration(long duration) {
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

    private void reset() {
        instrumentationList = new HashSet<>(64);
        requestSuccessCount = 0;
        requestFailureCount = 0;
        requestDurations = new ArrayList<>();
        retryCount = 0;
        throttleCount = 0;
        exceptionCount = 0;
    }
}
