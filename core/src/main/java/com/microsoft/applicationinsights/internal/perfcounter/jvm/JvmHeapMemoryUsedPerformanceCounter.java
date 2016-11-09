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

package com.microsoft.applicationinsights.internal.perfcounter.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounter;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

/**
 * The class will create a metric telemetry for capturing the Jvm's heap memory usage
 *
 * Created by gupele on 8/8/2016.
 */
public class JvmHeapMemoryUsedPerformanceCounter implements PerformanceCounter {

    public final static String NAME = "MemoryUsage";

        private final static String HEAP_MEM_USED = "Heap Memory Used (MB)";

    private final long Megabyte = 1024 * 1024;

    private final MemoryMXBean memory;

    public JvmHeapMemoryUsedPerformanceCounter() {
        memory = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public String getId() {
        return "JvmHeapMemoryUsedPerformanceCounter";
    }

    @Override
    public void report(TelemetryClient telemetryClient) {
        if (memory == null) {
            return;
        }

        reportHeap(memory, telemetryClient);
    }

    private void reportHeap(MemoryMXBean memory, TelemetryClient telemetryClient) {
        MemoryUsage mhu = memory.getHeapMemoryUsage();
        if (mhu != null) {
            long currentHeapUsed = mhu.getUsed() / Megabyte;
            MetricTelemetry memoryHeapUsage = new MetricTelemetry(HEAP_MEM_USED, currentHeapUsed);
            memoryHeapUsage.markAsCustomPerfCounter();
            telemetryClient.track(memoryHeapUsage);
        }
    }
}
