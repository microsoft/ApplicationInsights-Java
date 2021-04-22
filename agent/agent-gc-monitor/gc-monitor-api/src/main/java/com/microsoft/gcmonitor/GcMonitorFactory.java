package com.microsoft.gcmonitor;

import javax.management.MBeanServerConnection;
import java.util.concurrent.ExecutorService;

/**
 * Service loader interface
 */
public interface GcMonitorFactory {
    MemoryManagement monitorSelf(ExecutorService executorService, GCEventConsumer consumer) throws UnableToMonitorMemoryException;

    MemoryManagement monitor(MBeanServerConnection connection, ExecutorService executorService, GCEventConsumer consumer) throws UnableToMonitorMemoryException;
}
