// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.hardware.MemoryInfo;
import com.microsoft.applicationinsights.diagnostics.collection.libos.hardware.MemoryInfoReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupUsageDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.GlobalDiskStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelCounters;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelMonitorDeviceDriver;
import com.microsoft.applicationinsights.diagnostics.collection.libos.net.GlobalNetworkStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.net.TCPStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessCPUStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessIOStats;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/** Main entry point that brings together all data to be scraped */
public class SystemStatsReader implements Closeable {

  private final KernelMonitorDeviceDriver driver;

  @SuppressWarnings("checkstyle:MemberName")
  private final CGroupUsageDataReader cGroupUsageDataReader;

  private final ProcessCPUStats processCpuStats;
  private final MemoryInfoReader memoryInfoReader;
  private final ProcessIOStats ioStats;
  private boolean open;

  private final Object lock = new Object();

  public SystemStatsReader(
      KernelMonitorDeviceDriver driver,
      @SuppressWarnings("checkstyle:ParameterName") CGroupUsageDataReader cGroupUsageDataReader,
      ProcessCPUStats processCpuStats,
      ProcessIOStats ioStats,
      MemoryInfoReader memoryInfoReader) {
    this.open = true;
    this.driver = driver;
    this.cGroupUsageDataReader = cGroupUsageDataReader;
    this.processCpuStats = processCpuStats;
    this.ioStats = ioStats;
    this.memoryInfoReader = memoryInfoReader;
  }

  public List<Double> readTelemetry()
      throws OperatingSystemInteractionException, ReaderClosedException {
    synchronized (lock) {
      if (!open) {
        throw new ReaderClosedException();
      }

      driver.poll();
      cGroupUsageDataReader.poll();
      processCpuStats.poll();
      ioStats.poll();
      memoryInfoReader.poll();

      driver.update();
      cGroupUsageDataReader.update();
      processCpuStats.update();
      ioStats.update();
      memoryInfoReader.update();

      KernelCounters counters = driver.getCounters();
      GlobalDiskStats diskStats = driver.getDiskstats();
      GlobalNetworkStats netStats = driver.getNetworkStats();
      TCPStats tcpStats = driver.getTcpStats();

      return makeMap(
          diskStats,
          counters,
          tcpStats,
          netStats,
          processCpuStats,
          memoryInfoReader.getMemoryInfo(),
          ioStats,
          cGroupUsageDataReader.getTelemetry());
    }
  }

  public static List<Double> makeMap(
      GlobalDiskStats diskstats,
      KernelCounters counters,
      TCPStats tcpStats,
      GlobalNetworkStats netStats,
      ProcessCPUStats processCpuStats,
      MemoryInfo memoryInfo,
      ProcessIOStats ioStats,
      @Nullable List<Double> telemetry) {

    List<Double> data =
        Arrays.asList(
            (double) counters.getContextSwitches(),
            (double) counters.getProcsBlocked(),
            (double) counters.getProcsRunnable(),
            (double) tcpStats.getTotalReceivedQueuesSize(),
            (double) tcpStats.getTotalTransferredQueuesSize(),
            netStats.getTotalReceived().doubleValue(),
            netStats.getTotalWrite().doubleValue(),
            (double) memoryInfo.getTotalInKB(),
            (double) memoryInfo.getFreeInKB(),
            (double) memoryInfo.getVirtualMemoryTotalInKB(),
            (double) memoryInfo.getVirtualMemoryUsedInKB(),
            (double) counters.getUserTime(),
            (double) counters.getSystemTime(),
            (double) counters.getIdleTime(),
            (double) counters.getWaitTime(),
            (double) diskstats.getTotalRead(),
            (double) diskstats.getTotalWrite(),
            (double) diskstats.getTotalIO(),
            processCpuStats.getUserTime().doubleValue(),
            processCpuStats.getSystemTime().doubleValue(),
            processCpuStats.getPriority().doubleValue(),
            processCpuStats.getNice().doubleValue(),
            processCpuStats.getNumThreads().doubleValue(),
            processCpuStats.getVmSize().doubleValue(),
            processCpuStats.getRss().doubleValue(),
            processCpuStats.getSwapped().doubleValue(),
            ioStats.getDiskRead().doubleValue(),
            ioStats.getDiskWrite().doubleValue(),
            ioStats.getIoRead().doubleValue(),
            ioStats.getIoWrite().doubleValue());

    if (telemetry != null) {
      ArrayList<Double> tmp = new ArrayList<>();
      tmp.addAll(data);
      tmp.addAll(telemetry);
      data = tmp;
    }

    return Collections.unmodifiableList(data);
  }

  @Override
  public void close() throws IOException {
    synchronized (lock) {
      open = false;
      driver.close();
      cGroupUsageDataReader.close();
      processCpuStats.close();
      memoryInfoReader.close();
      ioStats.close();
    }
  }

  public boolean isOpen() {
    synchronized (lock) {
      return open;
    }
  }

  public static class ReaderClosedException extends Exception {}
}
