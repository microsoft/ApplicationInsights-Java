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

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import jdk.internal.joptsimple.internal.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.STATSBEAT_TELEMETRY_NAME;

abstract class BaseStatsbeat {

    private static final Logger logger = LoggerFactory.getLogger(BaseStatsbeat.class);
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(BaseStatsbeat.class));

    protected final TelemetryClient telemetryClient;

    BaseStatsbeat(TelemetryClient telemetryClient, long interval) {
        this.telemetryClient = telemetryClient;
        scheduledExecutor.scheduleWithFixedDelay(new StatsbeatSender(), interval, interval, TimeUnit.SECONDS);
    }

    protected abstract void send();

    protected MetricTelemetry createStatsbeatTelemetry(String name, double value) {
        MetricTelemetry telemetry = new MetricTelemetry(name, value);
        telemetry.setTelemetryName(STATSBEAT_TELEMETRY_NAME);
        telemetry.getContext().setInstrumentationKey(TelemetryConfiguration.getActive().getStatsbeatInstrumentationKey());
        CustomDimensions.get().populateProperties(telemetry.getProperties());
        return telemetry;
    }

    /**
     * Runnable which is responsible for calling the send method to transmit Statsbeat telemetry
     */
    private class StatsbeatSender implements Runnable {
        @Override
        public void run() {
            try {
                // For Linux Consumption Plan, connection string is lazily set.
                // There is no need to send statsbeat when cikey is empty.
                if (!Strings.isNullOrEmpty(CustomDimensions.get().getCustomerIkey())) {
                    return;
                }
                send();
            }
            catch (Exception e) {
                logger.error("Error occurred while sending statsbeat", e);
            }
        }
    }
}