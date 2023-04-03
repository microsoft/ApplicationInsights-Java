// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.kernel;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.TwoStepUpdatable;
import com.microsoft.applicationinsights.diagnostics.collection.libos.net.GlobalNetworkStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.net.TCPStats;
import java.io.Closeable;

/** Device driver for abstracting away any kernel specific statistics */
public interface KernelMonitorDeviceDriver extends Closeable, TwoStepUpdatable {

  KernelCounters getCounters() throws OperatingSystemInteractionException;

  GlobalNetworkStats getNetworkStats();

  GlobalDiskStats getDiskstats();

  TCPStats getTcpStats();
}
