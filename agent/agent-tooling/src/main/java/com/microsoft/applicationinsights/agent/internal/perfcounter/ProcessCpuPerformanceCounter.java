// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import static com.microsoft.applicationinsights.agent.internal.perfcounter.MetricNames.PROCESS_CPU_PERCENTAGE;
import static com.microsoft.applicationinsights.agent.internal.perfcounter.MetricNames.PROCESS_CPU_PERCENTAGE_NORMALIZED;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.CpuPerformanceCounterCalculator;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The class supplies the cpu usage of the Java process the SDK is in. */
public class ProcessCpuPerformanceCounter implements PerformanceCounter {

  private static final Logger logger = LoggerFactory.getLogger(ProcessCpuPerformanceCounter.class);

  private static final OperatingSystemMXBean operatingSystemMxBean =
      ManagementFactory.getOperatingSystemMXBean();

  private final boolean useNormalizedValueForNonNormalizedCpuPercentage;

  private final CpuPerformanceCounterCalculator cpuPerformanceCounterCalculator =
      getCpuPerformanceCounterCalculator();

  public ProcessCpuPerformanceCounter(boolean useNormalizedValueForNonNormalizedCpuPercentage) {
    this.useNormalizedValueForNonNormalizedCpuPercentage =
        useNormalizedValueForNonNormalizedCpuPercentage;
  }

  @Nullable
  private static CpuPerformanceCounterCalculator getCpuPerformanceCounterCalculator() {
    try {
      return new CpuPerformanceCounterCalculator();
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable t) {
      try {
        logger.error("Failed to create ProcessCpuPerformanceCounter", t);
        return null;
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t2) {
        // chomp
      }
      throw new IllegalStateException("Failed to create ProcessCpuPerformanceCounter", t);
    }
  }

  @Override
  public void report(TelemetryClient telemetryClient) {
    if (cpuPerformanceCounterCalculator == null) {
      return;
    }
    Double cpuPercentage = cpuPerformanceCounterCalculator.getCpuPercentage();
    if (cpuPercentage == null) {
      return;
    }

    double cpuPercentageNormalized = cpuPercentage / operatingSystemMxBean.getAvailableProcessors();

    if (useNormalizedValueForNonNormalizedCpuPercentage) {
      // use normalized for backwards compatibility even though this is supposed to be
      // non-normalized
      cpuPercentage = cpuPercentageNormalized;
    }

    logger.trace("Performance Counter: {}: {}", PROCESS_CPU_PERCENTAGE, cpuPercentage);
    telemetryClient.trackAsync(
        telemetryClient.newMetricTelemetry(PROCESS_CPU_PERCENTAGE, cpuPercentage));

    logger.trace(
        "Performance Counter: {}: {}", PROCESS_CPU_PERCENTAGE_NORMALIZED, cpuPercentageNormalized);
    telemetryClient.trackAsync(
        telemetryClient.newMetricTelemetry(
            PROCESS_CPU_PERCENTAGE_NORMALIZED, cpuPercentageNormalized));
  }
}
