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

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class will create the 'built-in'/default performance counters.
 *
 * Created by gupele on 3/3/2015.
 */
final class ProcessBuiltInPerformanceCountersFactory implements PerformanceCountersFactory {

    /**
     * Creates the {@link com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounter} that are
     * the 'built-in' performance counters of the process.
     *
     * Note: The method should not throw
     *
     * @return A collection of {@link com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounter}
     */
    public Collection<PerformanceCounter> getPerformanceCounters() {
        ArrayList<PerformanceCounter> performanceCounters = new ArrayList<>();
        performanceCounters.add(new ProcessCpuPerformanceCounter());
        performanceCounters.add(new ProcessMemoryPerformanceCounter());
        performanceCounters.add(new FreeMemoryPerformanceCounter());
        performanceCounters.add(new OshiPerformanceCounter()); // system cpu and process disk i/o
        return performanceCounters;
    }
}
