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

package com.microsoft.applicationinsights.internal.perfcounter;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.TelemetryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.applicationinsights.internal.perfcounter.Constants.PROCESS_CPU_PC_METRIC_NAME;
import static com.microsoft.applicationinsights.TelemetryUtil.createMetricsTelemetry;

/**
 * The class supplies the cpu usage of the Java process the SDK is in.
 */
final class ProcessCpuPerformanceCounter extends AbstractPerformanceCounter {

    private static final Logger logger = LoggerFactory.getLogger(ProcessCpuPerformanceCounter.class);

    private CpuPerformanceCounterCalculator cpuPerformanceCounterCalculator;

    public ProcessCpuPerformanceCounter() {
        try {
            cpuPerformanceCounterCalculator = new CpuPerformanceCounterCalculator();
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable t) {
            try {
                cpuPerformanceCounterCalculator = null;
                logger.error("Failed to create ProcessCpuPerformanceCounter", t);
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
            throw new IllegalStateException("Failed to create ProcessCpuPerformanceCounter", t);
        }
    }

    @Override
    public String getId() {
        return Constants.PROCESS_CPU_PC_ID;
    }

    @Override
    public void report(TelemetryClient telemetryClient) {
        if (cpuPerformanceCounterCalculator == null) {
            return;
        }
        Double processCpuUsage = cpuPerformanceCounterCalculator.getProcessCpuUsage();
        if (processCpuUsage == null) {
            return;
        }

        logger.trace("Performance Counter: {}: {}", PROCESS_CPU_PC_METRIC_NAME, processCpuUsage);
        TelemetryItem telemetry = createMetricsTelemetry(telemetryClient, PROCESS_CPU_PC_METRIC_NAME, processCpuUsage);
        telemetryClient.trackAsync(telemetry);
    }
}
