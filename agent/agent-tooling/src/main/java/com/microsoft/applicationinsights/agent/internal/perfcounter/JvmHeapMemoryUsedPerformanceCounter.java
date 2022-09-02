// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/** The class will create a metric telemetry for capturing the Jvm's heap memory usage. */
public class JvmHeapMemoryUsedPerformanceCounter implements PerformanceCounter {

  public static final String HEAP_MEM_USED = "Heap Memory Used (MB)";

  public static final String HEAP_MEM_USED_PERCENTAGE = "% Of Max Heap Memory Used";

  private static final double MEGABYTE = 1024 * 1024;

  private final MemoryMXBean memory;

  public JvmHeapMemoryUsedPerformanceCounter() {
    memory = ManagementFactory.getMemoryMXBean();
  }

  @Override
  public void report(TelemetryClient telemetryClient) {
    if (memory == null) {
      return;
    }

    reportHeap(memory, telemetryClient);
  }

  private static void reportHeap(MemoryMXBean memory, TelemetryClient telemetryClient) {
    MemoryUsage mhu = memory.getHeapMemoryUsage();
    if (mhu != null) {
      double currentHeapUsed = mhu.getUsed() / MEGABYTE;
      telemetryClient.trackAsync(
          telemetryClient.newMetricTelemetry(HEAP_MEM_USED, currentHeapUsed));

      float percentage = 100.0f * (((float) mhu.getUsed()) / ((float) mhu.getMax()));
      telemetryClient.trackAsync(
          telemetryClient.newMetricTelemetry(HEAP_MEM_USED_PERCENTAGE, percentage));
    }
  }
}
