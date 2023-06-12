// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.GlobalDiskStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelCounters;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelMonitorDeviceDriver;
import com.microsoft.applicationinsights.diagnostics.collection.libos.net.GlobalNetworkStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.net.TCPStats;
import java.io.IOException;

/** Brings together all the Linux readers to provide a Linux stats service */
public class LinuxKernelMonitor implements KernelMonitorDeviceDriver {

  private final LinuxKernelStats kernelStatsReader;

  private final LinuxGlobalNetworkStats netstats;

  private final LinuxGlobalDiskIoStats diskstats;
  private final LinuxTCPStatsReader linuxTcpStatsReader;

  public LinuxKernelMonitor() {
    diskstats = new LinuxGlobalDiskIoStats();
    netstats = new LinuxGlobalNetworkStats();
    kernelStatsReader = new LinuxKernelStats();
    linuxTcpStatsReader = new LinuxTCPStatsReader();
  }

  @Override
  public void close() throws IOException {
    kernelStatsReader.close();
    netstats.close();
    diskstats.close();
  }

  @Override
  public GlobalDiskStats getDiskstats() {
    return diskstats;
  }

  @Override
  public GlobalNetworkStats getNetworkStats() {
    return netstats;
  }

  @Override
  public KernelCounters getCounters() {
    return kernelStatsReader.getCounters();
  }

  @Override
  public TCPStats getTcpStats() {
    return linuxTcpStatsReader.getTCPStats();
  }

  @Override
  public void update() throws OperatingSystemInteractionException {
    diskstats.update();
    kernelStatsReader.update();
    netstats.update();
    linuxTcpStatsReader.update();
  }

  @Override
  public void poll() throws OperatingSystemInteractionException {
    diskstats.poll();
    netstats.poll();
    kernelStatsReader.poll();
    linuxTcpStatsReader.poll();
  }
}
