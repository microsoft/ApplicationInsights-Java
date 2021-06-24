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

package com.microsoft.applicationinsights.agent.internal.wascore.perfcounter;

import com.microsoft.applicationinsights.agent.internal.wascore.perfcounter.jvm.DeadLockDetectorPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.wascore.perfcounter.jvm.GcPerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.wascore.perfcounter.jvm.JvmHeapMemoryUsedPerformanceCounter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class will create dedicated Jvm performance counters, unless disabled by user in the
 * configuration file.
 */
public class JvmPerformanceCountersFactory implements PerformanceCountersFactory {

  private static final Logger logger = LoggerFactory.getLogger(JvmPerformanceCountersFactory.class);

  private boolean isEnabled = true;
  private HashSet<String> disabledJvmPerfCounters = new HashSet<>();

  @Override
  public Collection<PerformanceCounter> getPerformanceCounters() {
    ArrayList<PerformanceCounter> pcs = new ArrayList<>();
    if (isEnabled) {
      addDeadLockDetector(pcs);
      addJvmMemoryPerformanceCounter(pcs);
      addGcPerformanceCounter(pcs);
    } else {
      logger.trace("JvmPerformanceCountersFactory is disabled");
    }
    return pcs;
  }

  private void addDeadLockDetector(ArrayList<PerformanceCounter> pcs) {
    try {
      if (disabledJvmPerfCounters.contains(DeadLockDetectorPerformanceCounter.NAME)) {
        logger.trace("DeadLockDetectorPerformanceCounter is disabled");
        return;
      }

      DeadLockDetectorPerformanceCounter dlpc = new DeadLockDetectorPerformanceCounter();
      if (!dlpc.isSupported()) {
        logger.trace("DeadLockDetectorPerformanceCounter is not supported");
        return;
      }

      pcs.add(dlpc);
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable t) {
      try {
        logger.error("Failed to create DeadLockDetector", t);
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t2) {
        // chomp
      }
    }
  }

  private void addJvmMemoryPerformanceCounter(ArrayList<PerformanceCounter> pcs) {
    try {
      if (disabledJvmPerfCounters.contains(JvmHeapMemoryUsedPerformanceCounter.NAME)) {
        logger.trace("JvmHeapMemoryUsedPerformanceCounter is disabled");
        return;
      }

      JvmHeapMemoryUsedPerformanceCounter mpc = new JvmHeapMemoryUsedPerformanceCounter();
      pcs.add(mpc);
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable t) {
      try {
        logger.error("Failed to create JvmHeapMemoryUsedPerformanceCounter", t);
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t2) {
        // chomp
      }
    }
  }

  private void addGcPerformanceCounter(ArrayList<PerformanceCounter> pcs) {
    try {
      if (disabledJvmPerfCounters.contains(GcPerformanceCounter.NAME)) {
        return;
      }

      GcPerformanceCounter mpc = new GcPerformanceCounter();
      pcs.add(mpc);
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable t) {
      try {
        logger.error("Failed to create GCPerformanceCounter", t);
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t2) {
        // chomp
      }
    }
  }

  public void setIsEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  public void setDisabledJvmPerfCounters(HashSet<String> disabledJvmPerfCounters) {
    this.disabledJvmPerfCounters = disabledJvmPerfCounters;
  }
}
