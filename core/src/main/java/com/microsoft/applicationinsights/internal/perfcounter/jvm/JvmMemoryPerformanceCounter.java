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
 * Created by gupele on 8/8/2016.
 */
public class JvmMemoryPerformanceCounter implements PerformanceCounter {
    public final static String NAME = "MemoryUsage";

    private final static String HEAP_MEM_USED = "Jvm Heap Memory Used";
    private final static String HEAP_MEM_COMMITTED = "Jvm Heap Memory Committed";

    private final static String NON_HEAP_MEM_USED = "Jvm Non Heap Memory Used";
    private final static String NON_HEAP_MEM_COMMITTED = "Jvm Non Heap Memory Committed";

    @Override
    public String getId() {
        return "JvmHeapMemoryPerformanceCounter";
    }

    @Override
    public void report(TelemetryClient telemetryClient) {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        if (memory == null) {
            return;
        }
        reportHeap(memory, telemetryClient);
        reportNonHeap(memory, telemetryClient);

    }

    private void reportNonHeap(MemoryMXBean memory, TelemetryClient telemetryClient) {
        MemoryUsage mnhu = memory.getNonHeapMemoryUsage();
        if (mnhu == null) {
            return;
        }
        MetricTelemetry nonMemoryHeapUsage = new MetricTelemetry(NON_HEAP_MEM_USED, mnhu.getUsed());
        MetricTelemetry nonMemoryHeapCommitted = new MetricTelemetry(NON_HEAP_MEM_COMMITTED, mnhu.getCommitted());

        telemetryClient.track(nonMemoryHeapUsage);
        telemetryClient.track(nonMemoryHeapCommitted);
    }

    private void reportHeap(MemoryMXBean memory, TelemetryClient telemetryClient) {
        MemoryUsage mhu = memory.getHeapMemoryUsage();
        if (mhu != null) {
            MetricTelemetry memoryHeapUsage = new MetricTelemetry(HEAP_MEM_USED, mhu.getUsed());
            MetricTelemetry memoryHeapCommitted = new MetricTelemetry(HEAP_MEM_COMMITTED, mhu.getCommitted());
            telemetryClient.track(memoryHeapUsage);
            telemetryClient.track(memoryHeapCommitted);
        }
    }
}
