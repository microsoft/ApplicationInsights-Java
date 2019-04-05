package com.microsoft.applicationinsights.internal.util;

import com.microsoft.applicationinsights.internal.shutdown.SDKShutdownActivity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A utility clss to execute tasks on a schedule.
 */
public final class TimerTaskUtil {

    /**
     * A Map that keeps a list of registered tasks in the SDK.
     */
    private static final Map<String, ScheduledFuture<?>> executorServiceMap =
            new ConcurrentHashMap<>();

    private static final ScheduledExecutorService timerTaskService;

    private static final String TASK_POOL_NAME = "AI-SDK-TimerTask-Pool";

    private static final int poolSize;

    private TimerTaskUtil() {}

    public static void initializer() {
    }

    public static ScheduledFuture<?> executePeriodicTask(Runnable command, long initialDelay,
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

        ScheduledFuture<?> scheduledFuture = service.scheduleAtFixedRate(command, initialDelay, period, unit);
        executorServiceMap.put(taskId, scheduledFuture);
        SDKShutdownActivity.INSTANCE.register(service);
        return scheduledFuture;
    }


    public static class PeriodicTask {
        private final Runnable command;
        private final long initialDelay;
        private final long period;
        private final TimeUnit unit;
        private final Class cls;
        private final String taskId;


    }

    /**
     * USED ONLY FOR TESTING
     * Clear the map.
     */
    public static void reset() {
        executorServiceMap.clear();
    }

    /* Visible for Testing */
    static ScheduledFuture<?> getTask(String taskName) {
        return executorServiceMap.get(taskName);
    }

}

