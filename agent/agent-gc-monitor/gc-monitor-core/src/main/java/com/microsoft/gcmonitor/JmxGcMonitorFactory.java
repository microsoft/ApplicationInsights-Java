// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor;

import com.google.auto.service.AutoService;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import javax.management.MBeanServerConnection;

@AutoService(GcMonitorFactory.class)
public class JmxGcMonitorFactory implements GcMonitorFactory {

  @Override
  public MemoryManagement monitorSelf(ExecutorService executorService, GcEventConsumer consumer)
      throws UnableToMonitorMemoryException {
    return monitor(ManagementFactory.getPlatformMBeanServer(), executorService, consumer);
  }

  @Override
  public MemoryManagement monitor(
      MBeanServerConnection connection, ExecutorService executorService, GcEventConsumer consumer)
      throws UnableToMonitorMemoryException {
    return new JmxMemoryManagement()
        .init(connection, consumer)
        .monitorMxBeans(connection, executorService);
  }
}
