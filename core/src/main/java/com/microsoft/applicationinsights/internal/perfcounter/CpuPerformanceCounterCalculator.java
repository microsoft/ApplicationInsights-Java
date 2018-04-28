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

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;


/**
 * Created by gupele on 12/12/2016.
 */
public final class CpuPerformanceCounterCalculator {
    private final int numberOfCpus;

    private long prevUpTime, prevProcessCpuTime;

    private ObjectName osBean;

    public CpuPerformanceCounterCalculator() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        numberOfCpus = operatingSystemMXBean.getAvailableProcessors();

    }

    public Double getProcessCpuUsage() {
        Double processCpuUsage = null;
        try {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

            long upTime = runtimeMXBean.getUptime();
            long processCpuTime = getProcessCpuTime();

            if (prevUpTime > 0L && upTime > prevUpTime) {
                long elapsedCpu = processCpuTime - prevProcessCpuTime;
                long elapsedTime = upTime - prevUpTime;
                processCpuUsage = Math.min(99.999, elapsedCpu / (elapsedTime * 10_000.0 * numberOfCpus)); // if this looks weird, here's another way to write it: (elapsedCpu / 1000000.0) / elapsedTime / numberOfCpus * 100.0
            }
            prevUpTime = upTime;
            prevProcessCpuTime = processCpuTime;
        } catch (Exception e) {
            processCpuUsage = null;
            InternalLogger.INSTANCE.error("Error in getProcessCPUUsage");
            InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
        }

        return processCpuUsage;
    }

    private long getProcessCpuTime() throws Exception {
        MBeanServer bsvr = ManagementFactory.getPlatformMBeanServer();
        if (osBean == null) {
            osBean = ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        }
        return (Long) bsvr.getAttribute(osBean, "ProcessCpuTime");
    }
}
