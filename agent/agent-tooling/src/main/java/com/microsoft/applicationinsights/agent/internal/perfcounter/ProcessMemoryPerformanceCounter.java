// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import static com.microsoft.applicationinsights.agent.internal.perfcounter.MetricNames.PROCESS_MEMORY;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The class supplies the memory usage in Mega Bytes of the Java process the SDK is in. */
public class ProcessMemoryPerformanceCounter implements PerformanceCounter {

  private static final Logger logger =
      LoggerFactory.getLogger(ProcessMemoryPerformanceCounter.class);

  public ProcessMemoryPerformanceCounter() {}

  @Override
  public void report(TelemetryClient telemetryClient) {
    MemoryMXBean memoryData = ManagementFactory.getMemoryMXBean();

    MemoryUsage heapMemoryUsage = memoryData.getHeapMemoryUsage();
    MemoryUsage nonHeapMemoryUsage = memoryData.getNonHeapMemoryUsage();

    double memoryBytes = (double) heapMemoryUsage.getUsed();
    memoryBytes += (double) nonHeapMemoryUsage.getUsed();

    logger.trace("Performance Counter: {}: {}", PROCESS_MEMORY, memoryBytes);
    telemetryClient.trackAsync(telemetryClient.newMetricTelemetry(PROCESS_MEMORY, memoryBytes));
  }
}
