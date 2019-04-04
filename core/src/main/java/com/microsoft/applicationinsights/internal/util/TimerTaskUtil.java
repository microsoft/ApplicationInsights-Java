package com.microsoft.applicationinsights.internal.util;

import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A utility task to execute tasks on a schedule.
 */
public final class TimerTaskUtil {

    /**
     * A Map that keeps a list of registered tasks in the SDK.
     */
    private static final Map<String, ScheduledExecutorService> executorServiceMap =
            new HashMap<>();

    private TimerTaskUtil() {}

    public static void executePeriodicTask(Runnable command, long initialDelay,
                                                          long period, TimeUnit unit, Class cls, String taskId) {

        if (command == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        if (taskId == null || taskId.length() == 0) {
            throw new IllegalArgumentException("UniqueId of a task cannot be null or empty");
        }

        if (initialDelay < 0) {
            throw new IllegalArgumentException("illegal value of initial delay of task execution");
        }

        if (cls == null) {
            throw new IllegalArgumentException("Illegal value of parameter cls");
        }

        if (period <= 0) {
            throw new IllegalArgumentException("Illegal value of execution period. It cannot be 0 or less");
        }

        if (unit == null) {
            throw new IllegalArgumentException("Cannot have null TimeUnit");
        }

        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(
                ThreadPoolUtils.createDaemonThreadFactory(cls, taskId));
        if (executorServiceMap.containsKey(taskId)) {
            throw new IllegalStateException("Cannot have duplicate task Id's for tasks");
        }

        executorServiceMap.put(taskId, service);
        service.scheduleAtFixedRate(command, initialDelay, period, unit);
        SDKShutdownActivity.INSTANCE.register(service);
    }

    /**
     * USED ONLY FOR TESTING
     * Clear the map.
     */
    public static void reset() {
        executorServiceMap.clear();
    }

    static ScheduledExecutorService getServiceTaskName(String taskName) {
        return executorServiceMap.get(taskName);
    }

}

