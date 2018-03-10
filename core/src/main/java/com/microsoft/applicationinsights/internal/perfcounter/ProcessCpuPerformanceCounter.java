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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * The class supplies the cpu usage of the Java process the SDK is in.
 *
 * Created by gupele on 3/3/2015.
 */
final class ProcessCpuPerformanceCounter extends AbstractPerformanceCounter {

    private CpuPerformanceCounterCalculator cpuPerformanceCounterCalculator;

    public ProcessCpuPerformanceCounter() {
        try {
            cpuPerformanceCounterCalculator = new CpuPerformanceCounterCalculator();
        } catch (Throwable t) {
            cpuPerformanceCounterCalculator = null;
            InternalLogger.INSTANCE.error("Failed to create ProcessCpuPerformanceCounter: %s", ExceptionUtils.getStackTrace(t));
            InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(t));
            throw new RuntimeException("Failed to create ProcessCpuPerformanceCounter");
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
        double processCpuUsage = cpuPerformanceCounterCalculator.getProcessCpuUsage();

        InternalLogger.INSTANCE.trace("Performance Counter: %s %s: %s", getProcessCategoryName(), Constants.CPU_PC_COUNTER_NAME, processCpuUsage);
        Telemetry telemetry = new PerformanceCounterTelemetry(
                getProcessCategoryName(),
                Constants.CPU_PC_COUNTER_NAME,
                SystemInformation.INSTANCE.getProcessId(),
                processCpuUsage);
        telemetryClient.track(telemetry);
    }
}
