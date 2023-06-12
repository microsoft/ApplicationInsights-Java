// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop;

import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.GlobalDiskStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelCounters;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelMonitorDeviceDriver;
import com.microsoft.applicationinsights.diagnostics.collection.libos.net.GlobalNetworkStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.net.TCPStats;
import java.io.IOException;
import java.math.BigInteger;

public class NoOpKernelMonitor implements KernelMonitorDeviceDriver {

  public NoOpKernelMonitor() {}

  @Override
  public void close() throws IOException {}

  @Override
  public KernelCounters getCounters() {
    return new KernelCounters(-1, -1, -1, -1, -1, -1, -1);
  }

  @Override
  public GlobalNetworkStats getNetworkStats() {
    return new GlobalNetworkStats() {
      @Override
      public BigInteger getTotalWrite() {
        return BigInteger.valueOf(-1);
      }

      @Override
      public BigInteger getTotalReceived() {
        return BigInteger.valueOf(-1);
      }
    };
  }

  @Override
  public GlobalDiskStats getDiskstats() {
    return new GlobalDiskStats() {
      @Override
      public long getTotalWrite() {
        return -1;
      }

      @Override
      public long getTotalRead() {
        return -1;
      }

      @Override
      public long getTotalIO() {
        return -1;
      }
    };
  }

  @Override
  public TCPStats getTcpStats() {
    return new TCPStats(-1, -1);
  }

  @Override
  public void poll() {}

  @Override
  public void update() {}
}
