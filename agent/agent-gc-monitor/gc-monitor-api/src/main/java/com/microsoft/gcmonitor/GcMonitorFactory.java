// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor;

import java.util.concurrent.ExecutorService;
import javax.management.MBeanServerConnection;

/** Service loader interface. */
public interface GcMonitorFactory {
  MemoryManagement monitorSelf(ExecutorService executorService, GcEventConsumer consumer)
      throws UnableToMonitorMemoryException;

  MemoryManagement monitor(
      MBeanServerConnection connection, ExecutorService executorService, GcEventConsumer consumer)
      throws UnableToMonitorMemoryException;
}
