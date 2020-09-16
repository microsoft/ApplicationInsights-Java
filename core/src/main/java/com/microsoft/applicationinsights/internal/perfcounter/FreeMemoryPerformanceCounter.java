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

import java.lang.management.ManagementFactory;
import javax.management.ObjectName;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.system.SystemInformation;
import com.microsoft.applicationinsights.telemetry.PerformanceCounterTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class supplies the memory usage in Mega Bytes of the Java process the SDK is in.
 * <p>
 * Created by gupele on 3/3/2015.
 */
final class FreeMemoryPerformanceCounter extends AbstractPerformanceCounter {

    private static final Logger logger = LoggerFactory.getLogger(FreeMemoryPerformanceCounter.class);

    private ObjectName osBean;

    public FreeMemoryPerformanceCounter() {
    }

    @Override
    public String getId() {
        return Constants.TOTAL_MEMORY_PC_ID;
    }

    @Override
    public void report(TelemetryClient telemetryClient) {
        long freePhysicalMemorySize;
        try {
            freePhysicalMemorySize = getFreePhysicalMemorySize();
        } catch (Exception e) {
            logger.error("Error getting FreePhysicalMemorySize");
            logger.trace("Error getting FreePhysicalMemorySize", e);
            return;
        }

        logger.trace("Performance Counter: {} {}: {}", Constants.TOTAL_MEMORY_PC_CATEGORY_NAME,
                Constants.TOTAL_MEMORY_PC_COUNTER_NAME, freePhysicalMemorySize);
        Telemetry telemetry = new PerformanceCounterTelemetry(
                Constants.TOTAL_MEMORY_PC_CATEGORY_NAME,
                Constants.TOTAL_MEMORY_PC_COUNTER_NAME,
                "",
                freePhysicalMemorySize);

        telemetryClient.track(telemetry);
    }

    private long getFreePhysicalMemorySize() throws Exception {
        if (osBean == null) {
            osBean = ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        }
        return (Long) ManagementFactory.getPlatformMBeanServer().getAttribute(osBean, "FreePhysicalMemorySize");
    }
}
