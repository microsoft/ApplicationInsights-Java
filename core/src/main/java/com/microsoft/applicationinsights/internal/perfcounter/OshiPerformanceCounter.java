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

import static com.microsoft.applicationinsights.TelemetryUtil.createMetricsTelemetry;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.TelemetryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class OshiPerformanceCounter implements PerformanceCounter {

  private static final Logger logger = LoggerFactory.getLogger(OshiPerformanceCounter.class);
  private static final String ID = Constants.PERFORMANCE_COUNTER_PREFIX + "OshiPerformanceCounter";

  private static final double MILLIS_IN_SECOND = 1000;

  private long prevCollectionTimeMillis;
  private long prevProcessBytes;
  private long prevTotalProcessorMillis;

  private volatile OSProcess processInfo;
  private volatile CentralProcessor processor;

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public void report(TelemetryClient telemetryClient) {
    if (processInfo == null || processor == null) {
      // lazy initializing these because they add to slowness during startup
      SystemInfo systemInfo = new SystemInfo();
      OperatingSystem osInfo = systemInfo.getOperatingSystem();
      processInfo = osInfo.getProcess(osInfo.getProcessId());
      processor = systemInfo.getHardware().getProcessor();
    }

    long currCollectionTimeMillis = System.currentTimeMillis();
    long currProcessBytes = 0L;
    if (processInfo != null) {
      updateAttributes(processInfo);
      currProcessBytes = getProcessBytes(processInfo);
    }

    long currTotalProcessorMillis = getTotalProcessorMillis(processor);

    if (prevCollectionTimeMillis != 0) {
      double elapsedMillis = currCollectionTimeMillis - prevCollectionTimeMillis;
      double elapsedSeconds = elapsedMillis / MILLIS_IN_SECOND;
      if (processInfo != null) {
        double processBytes = (currProcessBytes - prevProcessBytes) / elapsedSeconds;
        send(telemetryClient, processBytes, Constants.PROCESS_IO_PC_METRIC_NAME);
        logger.trace(
            "Sent performance counter for '{}': '{}'",
            Constants.PROCESS_IO_PC_METRIC_NAME,
            processBytes);
      }

      double processorLoad =
          (currTotalProcessorMillis - prevTotalProcessorMillis)
              / (elapsedMillis * processor.getLogicalProcessorCount());
      double processorPercentage = 100 * processorLoad;
      send(telemetryClient, processorPercentage, Constants.TOTAL_CPU_PC_METRIC_NAME);
      logger.trace(
          "Sent performance counter for '{}': '{}'",
          Constants.TOTAL_CPU_PC_METRIC_NAME,
          processorPercentage);
    }

    prevCollectionTimeMillis = currCollectionTimeMillis;
    prevProcessBytes = currProcessBytes;
    prevTotalProcessorMillis = currTotalProcessorMillis;
  }

  private static void updateAttributes(OSProcess processInfo) {
    if (!processInfo.updateAttributes()) {
      logger.debug("could not update process attributes");
    }
  }

  // must call updateAttributes on processInfo before calling this method
  private static long getProcessBytes(OSProcess processInfo) {
    return processInfo.getBytesRead() + processInfo.getBytesWritten();
  }

  private static long getTotalProcessorMillis(CentralProcessor processor) {
    long[] systemCpuLoadTicks = processor.getSystemCpuLoadTicks();
    return systemCpuLoadTicks[TickType.USER.getIndex()]
        + systemCpuLoadTicks[TickType.SYSTEM.getIndex()];
  }

  private static void send(TelemetryClient telemetryClient, double value, String metricName) {
    TelemetryItem telemetry = createMetricsTelemetry(telemetryClient, metricName, value);
    telemetryClient.trackAsync(telemetry);
  }
}
