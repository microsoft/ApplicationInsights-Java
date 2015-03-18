/*
 * AppInsights-Java
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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * The class supplies the cpu usage of the Java process the SDK is in.
 *
 * Created by gupele on 3/3/2015.
 */
final class ProcessCpuPerformanceCounter extends AbstractPerformanceCounter {

    private final int numberOfCpus;

    private long prevUpTime, prevProcessCpuTime;
    private final String categoryName;

    public ProcessCpuPerformanceCounter() {
        com.sun.management.OperatingSystemMXBean operatingSystemMXBean = (com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        numberOfCpus = operatingSystemMXBean.getAvailableProcessors();
        categoryName = getProcessCategoryName();
    }

    @Override
    public String getId() {
        return Constants.PROCESS_CPU_PC_ID;
    }

    @Override
    public void report(TelemetryClient telemetryClient) {
        double processCpuUsage = getProcessCpuUsage();

        System.out.println(categoryName + " " + Constants.CPU_PC_COUNTER_NAME + " " + processCpuUsage);
        Telemetry telemetry = new PerformanceCounterTelemetry(
                categoryName,
                Constants.CPU_PC_COUNTER_NAME,
                "",
                processCpuUsage);
        telemetryClient.track(telemetry);
    }

    private double getProcessCpuUsage() {
        double processCpuUsage;
        try {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            com.sun.management.OperatingSystemMXBean operatingSystemMXBean =
                    (com.sun.management.OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();

            long upTime = runtimeMXBean.getUptime();
            long processCpuTime = operatingSystemMXBean.getProcessCpuTime();

            if (prevUpTime > 0L && upTime > prevUpTime) {
                long elapsedCpu = processCpuTime - prevProcessCpuTime;
                long elapsedTime = upTime - prevUpTime;
                processCpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * numberOfCpus));
            } else {
                processCpuUsage = Constants.DEFAULT_DOUBLE_VALUE;
            }
            prevUpTime = upTime;
            prevProcessCpuTime = processCpuTime;
        } catch (Exception e) {
            processCpuUsage = Constants.DEFAULT_DOUBLE_VALUE;
        }

        return processCpuUsage;
    }
}
