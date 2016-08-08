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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.perfcounter.PerformanceCounter;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;

import static java.lang.Math.min;

/**
 * The class uses the JVM ThreadMXBean to detect threads dead locks
 * A metric with value 0 is sent when there are no blocked threads,
 * otherwise the number of detected blocked threads is sent with a
 * dimension that holds information like thread id and minimal stack traces
 *
 * Created by gupele on 8/7/2016.
 */
public final class DeadLockDetectorPerformanceCounter implements PerformanceCounter {

    public final static String NAME = "ThreadDeadLockDetector";

    private final static String INDENT = "    ";
    private final static String SEPERATOR = " | ";
    private final static String METRIC_NAME = "Suspected deadlock Threads";
    private final static int MAX_STACK_TRACE = 3;

    private final ThreadMXBean threadBean;

    public DeadLockDetectorPerformanceCounter() {
        threadBean = ManagementFactory.getThreadMXBean();
    }

    public boolean isSupported() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return threadBean.isSynchronizerUsageSupported();
    }

    @Override
    public String getId() {
        return "DeadLockDetector";
    }

    @Override
    public void report(TelemetryClient telemetryClient) {
        MetricTelemetry mt = new MetricTelemetry(METRIC_NAME, 0.0);

        long[] threadIds = threadBean.findDeadlockedThreads();
        if (threadIds != null && threadIds.length > 0) {
            ArrayList<Long> blockedThreads = new ArrayList<Long>();

            StringBuilder sb = new StringBuilder();
            for (long threadId : threadIds) {
                ThreadInfo threadInfo = threadBean.getThreadInfo(threadId);
                if (threadInfo == null) {
                    continue;
                }

                setThreadInfoAndStack(sb, threadInfo);
                blockedThreads.add(threadId);
            }

            if (!blockedThreads.isEmpty()) {
                mt.setValue((double)blockedThreads.size());
                mt.getContext().getProperties().putIfAbsent("Suspected threads that are in dead lock", sb.toString());
            }
        }
        telemetryClient.track(mt);
    }
    private void setThreadInfoAndStack(StringBuilder sb, ThreadInfo ti) {
        try {
            setThreadInfo(sb, ti);

            // Stack traces up to depth of MAX_STACK_TRACE
            StackTraceElement[] stacktrace = ti.getStackTrace();
            MonitorInfo[] monitors = ti.getLockedMonitors();
            int maxTraceToReport = min(MAX_STACK_TRACE, stacktrace.length);
            for (int i = 0; i < maxTraceToReport; i++) {
                StackTraceElement ste = stacktrace[i];
                sb.append(INDENT + "at " + ste.toString());
                for (MonitorInfo mi : monitors) {
                    if (mi.getLockedStackDepth() == i) {
                        sb.append(INDENT + "  - is locked " + mi);
                    }
                }
            }
        } catch (Throwable t) {
        }
        sb.append(SEPERATOR);
    }

    private void setThreadInfo(StringBuilder sb, ThreadInfo ti) {
        sb.append(ti.getThreadName());
        sb.append(" Id=");
        sb.append(ti.getThreadId());
        sb.append(" in ");
        sb.append(ti.getThreadState());
        if (ti.getLockName() != null) {
            sb.append(" on lock=" + ti.getLockName());
        }
        if (ti.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (ti.isInNative()) {
            sb.append(" (running in native)");
        }
        if (ti.getLockOwnerName() != null) {
            sb.append(INDENT + " is owned by " + ti.getLockOwnerName() + " Id=" + ti.getLockOwnerId());
        }
    }
}
