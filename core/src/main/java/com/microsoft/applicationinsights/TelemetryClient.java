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

package com.microsoft.applicationinsights;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.azure.monitor.opentelemetry.exporter.implementation.ApplicationInsightsClientImpl;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulseDataCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.singletonList;

// Created by gupele
/**
 * Create an instance of this class to send telemetry to Azure Application Insights.
 * General overview https://docs.microsoft.com/azure/application-insights/app-insights-api-custom-events-metrics
 */
public class TelemetryClient {

    private static final Logger logger = LoggerFactory.getLogger(TelemetryClient.class);

    private static final AtomicLong generateCounter = new AtomicLong(0);

    private final TelemetryConfiguration configuration;

    public TelemetryClient() {
        this(TelemetryConfiguration.getActive());
    }

    public TelemetryClient(TelemetryConfiguration configuration) {


        this.configuration = configuration;
    }

    public void track(TelemetryItem telemetry) {

        if (generateCounter.incrementAndGet() % 10000 == 0) {
            logger.debug("Total events generated till now {}", generateCounter.get());
        }

        if (telemetry == null) {
            // TODO (trask) remove this after confident no code paths hit this
            throw new IllegalArgumentException("telemetry item cannot be null");
        }

        if (telemetry.getTime() == null) {
            // TODO (trask) remove this after confident no code paths hit this
            throw new IllegalArgumentException("telemetry item is missing time");
        }

        // FIXME (trask) need to handle this for OpenTelemetry exporter too
        QuickPulseDataCollector.INSTANCE.add(telemetry);

        ApplicationInsightsClientImpl channel = configuration.getChannel();

        try {
            // FIXME (trask) do something with return value, for flushing / shutdown purpose
            channel.trackAsync(singletonList(telemetry));
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            try {
                logger.error("Exception while sending telemetry: '{}'",t.toString());            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }

        // FIXME (trask) need to handle this for OpenTelemetry exporter too
        TelemetryObservers.INSTANCE.getObservers().forEach(consumer -> consumer.accept(telemetry));
    }

    /**
     * Flushes possible pending Telemetries in the channel. Not required for a continuously-running server application.
     */
    public void flush() {
        // FIXME (trask)
        // getChannel().flush();
    }

    public void shutdown(long timeout, TimeUnit timeUnit) throws InterruptedException {
        // FIXME (trask)
        // getChannel().shutdown(timeout, timeUnit);
    }
}
