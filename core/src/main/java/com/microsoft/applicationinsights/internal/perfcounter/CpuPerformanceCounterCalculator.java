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

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.OperatingSystemMXBean;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;


/**
 * Created by gupele on 12/12/2016.
 */
public final class CpuPerformanceCounterCalculator {
    private final int numberOfCpus;

    private long prevUpTime, prevProcessCpuTime;

    public CpuPerformanceCounterCalculator() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        numberOfCpus = operatingSystemMXBean.getAvailableProcessors();
    }

    public double getProcessCpuUsage() {
        double processCpuUsage;
        try {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

            long upTime = runtimeMXBean.getUptime();
            long processCpuTime = getProcessCpuTime();

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

    private long getProcessCpuTime() throws Exception {
        MBeanServer bsvr = ManagementFactory.getPlatformMBeanServer();
        ObjectName oname = ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        return (Long)bsvr.getAttribute(oname, "ProcessCpuTime");
    }
}
