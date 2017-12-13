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

package com.microsoft.applicationinsights.internal.quickpulse;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.perfcounter.CpuPerformanceCounterCalculator;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Created by gupele on 12/5/2016.
 */
public enum QuickPulseDataCollector {
    INSTANCE;

    private String ikey;

    static class FinalCounters {
        public final double exceptions;
        public final long requests;
        public final double requestsDuration;
        public final long unsuccessfulRequests;
        public final long rdds;
        public final double rddsDuration;
        public final long unsuccessfulRdds;
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

            CountAndDuration countAndDuration = currentCounters.decodeCountAndDuration(currentCounters.requestsAndDurations.get());
            requests = countAndDuration.count;
            this.requestsDuration = countAndDuration.duration;
            this.unsuccessfulRequests = currentCounters.unsuccessfulRequests.get();

            countAndDuration = currentCounters.decodeCountAndDuration(currentCounters.rddsAndDuations.get());
            this.rdds = countAndDuration.count;
            this.rddsDuration = countAndDuration.duration;
            this.unsuccessfulRdds = currentCounters.unsuccessfulRdds.get();
        }
    }

    private static class CountAndDuration {
        public final long count;
        public final long duration;

        private CountAndDuration(long count, long duration) {
            this.count = count;
            this.duration = duration;
        }
    }

    private static class Counters {
        private final static long MAX_COUNT = 524287L;
        private final static long MAX_DURATION = 17592186044415L;

        public AtomicInteger exceptions = new AtomicInteger(0);

        public AtomicLong requestsAndDurations = new AtomicLong(0);
        public AtomicInteger unsuccessfulRequests = new AtomicInteger(0);

        public AtomicLong rddsAndDuations = new AtomicLong(0);
        public AtomicInteger unsuccessfulRdds = new AtomicInteger(0);

        public static long encodeCountAndDuration(long  count, long duration) {
            if (count > MAX_COUNT || duration > MAX_DURATION) {
                return 0;
            }

            return (count << 44) + duration;
        }

        public static CountAndDuration decodeCountAndDuration(long countAndDuration) {
            return new CountAndDuration(countAndDuration >> 44, countAndDuration & MAX_DURATION);
        }
    }

    private AtomicReference<Counters> counters = new AtomicReference<Counters>(null);
    private final MemoryMXBean memory;
    private final CpuPerformanceCounterCalculator cpuPerformanceCounterCalculator;

    QuickPulseDataCollector() {
        CpuPerformanceCounterCalculator temp;
        try {
            temp = new CpuPerformanceCounterCalculator();
        } catch (Throwable t) {
            InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(t));
            temp = null;
        }
        cpuPerformanceCounterCalculator = temp;
        memory = ManagementFactory.getMemoryMXBean();
    }

    public synchronized void disable() {
        counters.set(null);
    }

    public synchronized void enable(final String ikey) {
        this.ikey = ikey;
        counters.set(new Counters());
    }

    public FinalCounters getAndRestart() {
        final Counters currentCounters = counters.getAndSet(new Counters());
        if (currentCounters != null) {
            return new FinalCounters(currentCounters, memory, cpuPerformanceCounterCalculator);
        }

        return null;
    }

    public void add(Telemetry telemetry) {
        if (!telemetry.getContext().getInstrumentationKey().equals(ikey)) {
            return;
        }

        if (telemetry instanceof RequestTelemetry) {
            RequestTelemetry requestTelemetry = (RequestTelemetry)telemetry;
            addRequest(requestTelemetry);
        } else if (telemetry instanceof RemoteDependencyTelemetry) {
            addDependency((RemoteDependencyTelemetry) telemetry);
        } else if (telemetry instanceof ExceptionTelemetry) {
            addException();
        }
    }

    private void addDependency(RemoteDependencyTelemetry telemetry) {
    }

    private void addException() {
        Counters counters = this.counters.get();
        if (counters == null) {
            return;
        }

        counters.exceptions.incrementAndGet();
    }

    private void addRequest(RequestTelemetry requestTelemetry) {
        Counters counters = this.counters.get();
        if (counters == null) {
            return;
        }

        counters.requestsAndDurations.addAndGet(Counters.encodeCountAndDuration(1, requestTelemetry.getDuration().getMilliseconds()));
        if (!requestTelemetry.isSuccess()) {
            counters.unsuccessfulRequests.incrementAndGet();
        }
    }
}
