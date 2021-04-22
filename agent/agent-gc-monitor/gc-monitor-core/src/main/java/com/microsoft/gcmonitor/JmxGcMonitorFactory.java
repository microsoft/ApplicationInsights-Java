package com.microsoft.gcmonitor;

import javax.management.MBeanServerConnection;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;

public class JmxGcMonitorFactory implements GcMonitorFactory {

    @Override
    public MemoryManagement monitorSelf(ExecutorService executorService, GCEventConsumer consumer) throws UnableToMonitorMemoryException {
        return monitor(ManagementFactory.getPlatformMBeanServer(), executorService, consumer);
    }

    @Override
    public MemoryManagement monitor(MBeanServerConnection connection, ExecutorService executorService, GCEventConsumer consumer) throws UnableToMonitorMemoryException {
        return new JMXMemoryManagement()
                .init(connection, consumer)
                .monitorMxBeans(connection, executorService);
    }
}
