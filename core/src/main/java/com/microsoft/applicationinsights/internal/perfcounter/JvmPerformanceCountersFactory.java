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
import java.util.HashSet;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.perfcounter.jvm.DeadLockDetectorPerformanceCounter;
import com.microsoft.applicationinsights.internal.perfcounter.jvm.JvmHeapMemoryUsedPerformanceCounter;

/**
 * The class will create dedicated Jvm performance counters, unless disabled by user in the configuration file
 *
 * Created by gupele on 8/8/2016.
 */
public class JvmPerformanceCountersFactory implements PerformanceCountersFactory {
    private boolean isEnabled = true;
    private HashSet<String> disabledJvmPCs = new HashSet<String>();

    @Override
    public Collection<PerformanceCounter> getPerformanceCounters() {
        ArrayList<PerformanceCounter> pcs = new ArrayList<PerformanceCounter>();
        if (isEnabled) {
            addDeadLockDetector(pcs);
            addJvmMemoryPerformanceCounter(pcs);
        } else {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.TRACE, "JvmPerformanceCountersFactory is disabled");
        }
        return pcs;
    }

    private void addDeadLockDetector(ArrayList<PerformanceCounter> pcs) {
        try {
            if (disabledJvmPCs.contains(DeadLockDetectorPerformanceCounter.NAME)) {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.TRACE, "DeadLockDetectorPerformanceCounter is disabled");
                return;
            }

            DeadLockDetectorPerformanceCounter dlpc = new DeadLockDetectorPerformanceCounter();
            if (!dlpc.isSupported()) {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.TRACE, "DeadLockDetectorPerformanceCounter is not supported");
                return;
            }

            pcs.add(dlpc);
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            try {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Failed to create DeadLockDetector, exception: %s", t.toString());            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
    }

    private void addJvmMemoryPerformanceCounter(ArrayList<PerformanceCounter> pcs) {
        try {
            if (disabledJvmPCs.contains(JvmHeapMemoryUsedPerformanceCounter.NAME)) {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.TRACE, "JvmHeapMemoryUsedPerformanceCounter is disabled");
                return;
            }

            JvmHeapMemoryUsedPerformanceCounter mpc = new JvmHeapMemoryUsedPerformanceCounter();
            pcs.add(mpc);
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            try {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Failed to create JvmHeapMemoryUsedPerformanceCounter, exception: %s", t.toString());            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
    }

    public void setIsEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setDisabledJvmPCs(HashSet<String> disabledJvmPCs) {
        this.disabledJvmPCs = disabledJvmPCs;
    }

}
