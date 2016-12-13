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
import java.lang.management.MemoryMXBean;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;

/**
 * Created by gupele on 12/5/2016.
 */
public enum QuickPulseDataCollector {
    INSTANCE;

    public static class FinalCounters {
        public final double exceptions;
        public final int requests;
        public final double requestsDuration;
        public final int unsuccessfulRequests;
        public final int rdds;
        public final double rddsDuration;
        public final int unsuccessfulRdds;
        public final long memoryCommitted;
        public final double cpuUsage;

        public FinalCounters(Counters currentCounters, MemoryMXBean memory, CpuPerformanceCounterCalculator cpuPerformanceCounterCalculator) {
            if (memory != null && memory.getHeapMemoryUsage() != null) {
                memoryCommitted = memory.getHeapMemoryUsage().getCommitted();
            } else {
                memoryCommitted = -1;
            }
            if (cpuPerformanceCounterCalculator != null) {
                cpuUsage = cpuPerformanceCounterCalculator.getProcessCpuUsage();
            } else {
                cpuUsage = -1;
            }
            exceptions = currentCounters.exceptions.get();
            requests = currentCounters.requests.get();
            double temp = currentCounters.requestsDuration.get();
            if (requests != 0) {
                this.requestsDuration = temp / (double)this.requests;
            } else {
                this.requestsDuration = 0.0;
            }
            this.unsuccessfulRequests = currentCounters.unsuccessfulRequests.get();
            this.rdds = currentCounters.rdds.get();
            temp = currentCounters.rddsDuration.get();
            if (rdds != 0) {
                this.rddsDuration = temp / (double)rdds;
            } else {
                this.rddsDuration = 0.0;
            }
            this.unsuccessfulRdds = currentCounters.unsuccessfulRdds.get();
        }
    }

    private static class Counters {
        public AtomicInteger exceptions = new AtomicInteger(0);

        public AtomicInteger requests = new AtomicInteger(0);
        public AtomicLong requestsDuration = new AtomicLong(0);
        public AtomicInteger unsuccessfulRequests = new AtomicInteger(0);

        public AtomicInteger rdds = new AtomicInteger(0);
        public AtomicLong rddsDuration = new AtomicLong(0);
        public AtomicInteger unsuccessfulRdds = new AtomicInteger(0);
    }

    private AtomicReference<Counters> counters = new AtomicReference<Counters>(null);
    private final MemoryMXBean memory;
    private final CpuPerformanceCounterCalculator cpuPerformanceCounterCalculator;

    QuickPulseDataCollector() {
        CpuPerformanceCounterCalculator temp;
        try {
            temp = new CpuPerformanceCounterCalculator();
        } catch (Throwable t) {
            temp = null;
        }
        cpuPerformanceCounterCalculator = temp;
        memory = ManagementFactory.getMemoryMXBean();
    }

    public synchronized void disable() {
        counters.set(null);
    }

    public synchronized void enable() {
        counters.set(new Counters());
    }

    public FinalCounters getAndRestart() {
        final Counters currentCounters = counters.getAndSet(new Counters());
        if (currentCounters != null) {
            return new FinalCounters(currentCounters, memory, cpuPerformanceCounterCalculator);
        }

        return null;
    }

    public void countRequest(RequestTelemetry request) {
        addRequest(request.isSuccess(), request.getDuration().getMilliseconds());
    }

    public void add(Telemetry telemetry) {
        if (telemetry instanceof RequestTelemetry) {
            RequestTelemetry requestTelemetry = (RequestTelemetry)telemetry;
            addRequest(requestTelemetry.isSuccess(), requestTelemetry.getDuration().getMilliseconds());
        } else if (telemetry instanceof RemoteDependencyTelemetry) {
            addDependency((RemoteDependencyTelemetry)telemetry);
        }
    }

    public void addDependency(RemoteDependencyTelemetry telemetry) {
    }

    public void addException() {
        Counters counters = this.counters.get();
        if (counters == null) {
            return;
        }

        counters.exceptions.incrementAndGet();
    }

    public void addRequest(boolean isSuccessful, long duration) {
        Counters counters = this.counters.get();
        if (counters == null) {
            return;
        }

        counters.requests.incrementAndGet();
        counters.requestsDuration.addAndGet(duration);
        if (!isSuccessful) {
            counters.unsuccessfulRequests.incrementAndGet();
        }
    }

    public void addDependency(boolean isSuccessful, long duration) {
        Counters counters = this.counters.get();
        if (counters == null) {
            return;
        }

        counters.rdds.incrementAndGet();
        counters.rddsDuration.addAndGet(duration);
        if (!isSuccessful) {
            counters.unsuccessfulRdds.incrementAndGet();
        }
    }
}
