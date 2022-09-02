// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import static com.microsoft.applicationinsights.agent.bootstrap.diagnostics.MsgId.FREE_MEMORY_METRIC_ERROR;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** The class supplies the memory usage in Mega Bytes of the Java process the SDK is in. */
public class FreeMemoryPerformanceCounter implements PerformanceCounter {

  private static final Logger logger = LoggerFactory.getLogger(FreeMemoryPerformanceCounter.class);

  private ObjectName osBean;

  public FreeMemoryPerformanceCounter() {}

  @Override
  public void report(TelemetryClient telemetryClient) {
    long freePhysicalMemorySize;
    try {
      freePhysicalMemorySize = getFreePhysicalMemorySize();
    } catch (Exception e) {
      try (MDC.MDCCloseable ignored = FREE_MEMORY_METRIC_ERROR.makeActive()) {
        logger.error("Error getting FreePhysicalMemorySize");
      }
      logger.trace("Error getting FreePhysicalMemorySize", e);
      return;
    }

    logger.trace("Performance Counter: {}: {}", MetricNames.TOTAL_MEMORY, freePhysicalMemorySize);
    telemetryClient.trackAsync(
        telemetryClient.newMetricTelemetry(
            MetricNames.TOTAL_MEMORY, (double) freePhysicalMemorySize));
  }

  private long getFreePhysicalMemorySize() throws Exception {
    if (osBean == null) {
      osBean = ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
    }
    return (Long)
        ManagementFactory.getPlatformMBeanServer().getAttribute(osBean, "FreePhysicalMemorySize");
  }
}
