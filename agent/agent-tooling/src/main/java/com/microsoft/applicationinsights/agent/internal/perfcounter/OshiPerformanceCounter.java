// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOSProcess;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

public class OshiPerformanceCounter implements PerformanceCounter {

  private static final Logger logger = LoggerFactory.getLogger(OshiPerformanceCounter.class);

  private static final double MILLIS_IN_SECOND = 1000;

  private long prevCollectionTimeMillis;
  private long prevProcessBytes;
  private long prevTotalProcessorMillis;

  private volatile OSProcess processInfo;
  private volatile CentralProcessor processor;
  private static final AtomicBoolean hasError = new AtomicBoolean();

  @Override
  public void report(TelemetryClient telemetryClient) {
    // stop collecting oshi perf counters when initialization fails.
    if (hasError.get()) {
      return;
    }

    if (processInfo == null || processor == null) {
      // lazy initializing these because they add to slowness during startup
      try {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem osInfo = systemInfo.getOperatingSystem();
        processInfo = osInfo.getProcess(osInfo.getProcessId());
        processor = systemInfo.getHardware().getProcessor();
      } catch (Error | RuntimeException e) {
        // e.g. icm 253155448: NoClassDefFoundError
        // e.g. icm 276640835: ExceptionInInitializerError
        // e.g. icm 332200073: PdhUtil$PdhException
        hasError.set(true);
        logger.debug("Fail to initialize OSProcess and CentralProcessor", e);
        return;
      }
    }

    long currCollectionTimeMillis = System.currentTimeMillis();
    long currProcessBytes = 0L;
    if (processInfo != null) {
      if (processInfo instanceof LinuxOSProcess) {
        currProcessBytes = getProcessBytesLinux(processInfo.getProcessID());
      } else {
        updateAttributes(processInfo);
        currProcessBytes = getProcessBytes(processInfo);
      }
    }

    long currTotalProcessorMillis = getTotalProcessorMillis(processor);

    if (prevCollectionTimeMillis != 0) {
      double elapsedMillis = currCollectionTimeMillis - prevCollectionTimeMillis;
      double elapsedSeconds = elapsedMillis / MILLIS_IN_SECOND;
      if (processInfo != null) {
        double processBytes = (currProcessBytes - prevProcessBytes) / elapsedSeconds;
        send(telemetryClient, processBytes, MetricNames.PROCESS_IO);
        logger.trace(
            "Sent performance counter for '{}': '{}'", MetricNames.PROCESS_IO, processBytes);
      }

      double processorLoad =
          (currTotalProcessorMillis - prevTotalProcessorMillis)
              / (elapsedMillis * processor.getLogicalProcessorCount());
      double processorPercentage = 100 * processorLoad;
      send(telemetryClient, processorPercentage, MetricNames.TOTAL_CPU_PERCENTAGE);
      logger.trace(
          "Sent performance counter for '{}': '{}'",
          MetricNames.TOTAL_CPU_PERCENTAGE,
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

  // oshi.software.os.linux.LinuxOSProcess.updateAttributes() captures too much stuff each time,
  // including groupId which calls "getent group", which having the machine connected to active
  // directory, scans its entire structure
  // (https://portal.microsofticm.com/imp/v3/incidents/details/289056966/home)
  //
  // so this method copies what LinuxOSProcess.updateAttributes() does for just the metrics that we
  // use for calculating I/O bytes
  private static long getProcessBytesLinux(int processId) {
    Map<String, String> io =
        FileUtil.getKeyValueMapFromFile(String.format(ProcPath.PID_IO, processId), ":");
    long bytesRead = ParseUtil.parseLongOrDefault(io.getOrDefault("read_bytes", ""), 0L);
    long bytesWritten = ParseUtil.parseLongOrDefault(io.getOrDefault("write_bytes", ""), 0L);
    return bytesRead + bytesWritten;
  }

  private static long getTotalProcessorMillis(CentralProcessor processor) {
    long[] systemCpuLoadTicks = processor.getSystemCpuLoadTicks();
    return systemCpuLoadTicks[TickType.USER.getIndex()]
        + systemCpuLoadTicks[TickType.SYSTEM.getIndex()];
  }

  private static void send(TelemetryClient telemetryClient, double value, String metricName) {
    telemetryClient.trackAsync(telemetryClient.newMetricTelemetry(metricName, value));
  }
}
