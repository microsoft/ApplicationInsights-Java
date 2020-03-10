package com.microsoft.applicationinsights.internal.util;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.shutdown.Stoppable;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A class to execute tasks on a schedule. This class should be used to run and manage all the
 * scheduled Tasks in SDK.
 *
 * <h3>Usage example</h3>
 *
 * Here is a class that uses PeriodicTaskManager to execute {@link PeriodicRunnableTask}
 *
 * <pre> {@code
 * import com.microsoft.applicationinsights.internal.util.PeriodicTaskPool;
 * public class BeeperControl {
 *     public void beepForHour() {
 *        final Runnable beeper = new Runnable() {
 *            public void run() { System.out.println("beep"); }
 *         };
 *
 *         // create the PeriodicRunnableTask from runnable
 *         PeriodicTaskPool.PeriodicRunnableTask periodicTask = PeriodicTaskPool.PeriodicRunnableTask.getInstance(beeper,
 *                 0, 1, TimeUnit.SECONDS, PeriodicTaskManager.class,
 *                 "Beeper");
 *         ScheduledFuture<?> future = periodicTaskPool.executePeriodicRunnableTask(periodicTask);
 *
 *         // Cancel the PeriodicRunnableTask
 *         PeriodicTaskManager.INSTANCE.cancelPeriodicTask(periodicTask);
 *     }
 * }}</pre>
 * @since 2.4.0
 */
public class PeriodicTaskPool implements Stoppable {

    /**
     * A Map which stores the currently active PeriodicTasks and it's associate future.
     */
    private final Map<PeriodicRunnableTask, ScheduledFuture<?>> periodicTaskMap;

    /**
     * The executor service which is responsible for running the tasks
     */
    private final ScheduledExecutorService periodicTaskService;

    /**
     * Number of threads in the TheadPool associated with periodicTaskService.
     */
    private final int poolSize;

    public PeriodicTaskPool(int poolSize, String poolName) {
        if (poolSize < 1) {
            throw new IllegalArgumentException("ThreadPool size should be at least 1.");
        }
        if (StringUtils.isBlank(poolName)) {
            throw new IllegalArgumentException("poolName must be non-empty");
        }
        this.poolSize = poolSize;
        this.periodicTaskService = new ScheduledThreadPoolExecutor(this.poolSize,
                ThreadPoolUtils.createNamedDaemonThreadFactory(poolName));
        this.periodicTaskMap = new ConcurrentHashMap<>();
    }

    /**
     * Executes a {@link PeriodicRunnableTask}
     * @param task PeriodicRunnableTask
     * @return ScheduledFuture associated with the scheduled task.
     */
    public ScheduledFuture<?> executePeriodicRunnableTask(PeriodicRunnableTask task) {

        if (task == null) {
            throw new NullPointerException(" Task cannot be null");
        }

        if (periodicTaskMap.containsKey(task)) {
            throw new IllegalStateException("Cannot have duplicate tasks");
        }

        ScheduledFuture<?> scheduledFuture = periodicTaskService.scheduleAtFixedRate(task.getCommand(),
                task.getInitialDelay(), task.getPeriod(), task.getUnit());

        periodicTaskMap.put(task, scheduledFuture);
        return scheduledFuture;
    }

    /**
     * Cancels the task, if it's running and removes it from the periodicTaskMap.
     * @param task PeriodicRunnableTask
     * @return true if task is cancelled successfully
     */
    public boolean cancelPeriodicTask(PeriodicRunnableTask task) {

        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }

        if (!periodicTaskMap.containsKey(task)) {
            InternalLogger.INSTANCE.error(String.format("No such Task %s running",task));
            return false;
        }

        ScheduledFuture<?> futureToCancel = periodicTaskMap.get(task);

        if (futureToCancel.isCancelled() || futureToCancel.isDone()) {
            InternalLogger.INSTANCE.info("Cannot cancel task %s, It is either completed or already cancelled",
                    task);
            return false;
        }
        periodicTaskMap.remove(task);
        return futureToCancel.cancel(true);
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        periodicTaskService.shutdown();
        try {
            if (!periodicTaskService.awaitTermination(timeout, timeUnit)) {
                periodicTaskService.shutdownNow();
            }
        } catch (InterruptedException e) {
            periodicTaskService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            stopAndClear();
        }
    }


    /**
     * A Class that holds the instance of {@link Runnable} command along with it's unique taskId, initial delay,
     * repetition period and TimeUnit of repetition period
     */
    public static final class PeriodicRunnableTask {
        private final Runnable command;
        private final long initialDelay;
        private final long period;
        private final TimeUnit unit;
        private final String taskId;

        private PeriodicRunnableTask(Runnable command, long initialDelay, long period, TimeUnit unit, String taskId) {
            validate(command, initialDelay, period, unit, taskId);
            this.command = command;
            this.initialDelay = initialDelay;
            this.period = period;
            this.unit = unit;
            this.taskId = taskId;
        }

        /**
         * Creates a PeriodicRunnableTask
         * @param command The Runnable to execute
         * @param initialDelay initial delay before running task for the first time.
         * @param period after initial delay, period to execute task.
         * @param unit timeUnit for initial delay and period
         * @param taskId identifier for task
         * @return
         */
        public static PeriodicRunnableTask createTask(Runnable command, long initialDelay, long period,
                                                      TimeUnit unit, String taskId) {
            return new PeriodicRunnableTask(command, initialDelay, period, unit, taskId);
        }

        /**
         * Method to validate if the properties of task are valid.
         */
        private static void validate(Runnable command, long initialDelay, long period,
                                     TimeUnit unit, String taskId) {
            if (command == null) {
                throw new IllegalArgumentException("Task cannot be null");
            }

            if (taskId == null || taskId.length() == 0) {
                throw new IllegalArgumentException("UniqueId of a task cannot be null or empty");
            }

            if (initialDelay < 0) {
                throw new IllegalArgumentException("illegal value of initial delay of task execution");
            }

            if (period <= 0) {
                throw new IllegalArgumentException("Illegal value of execution period. It cannot be 0 or less");
            }

            if (unit == null) {
                throw new IllegalArgumentException("Cannot have null TimeUnit");
            }
        }

        public Runnable getCommand() {
            return command;
        }

        public long getInitialDelay() {
            return initialDelay;
        }

        public long getPeriod() {
            return period;
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public String getTaskId() {
            return taskId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PeriodicRunnableTask that = (PeriodicRunnableTask) o;
            return taskId.equals(that.taskId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId);
        }

        @Override
        public String toString() {
            return "PeriodicRunnableTask{" +
                    "command=" + command +
                    ", initialDelay=" + initialDelay +
                    ", period=" + period +
                    ", unit=" + unit +
                    ", taskId='" + taskId + '\'' +
                    '}';
        }
    }

    /**
     * Stop all the tasks and removes them from the collection.
     */
    public void stopAndClear() {
        for (Map.Entry<PeriodicRunnableTask, ScheduledFuture<?>> entry : periodicTaskMap.entrySet()) {
            ScheduledFuture<?> futureToRemove = entry.getValue();
            if (!futureToRemove.isDone() && !futureToRemove.isCancelled()) {
                futureToRemove.cancel(true);
            }
            periodicTaskMap.remove(entry.getKey());

        }
    }

    /* Visible for Testing */
    ScheduledFuture<?> getTask(PeriodicRunnableTask task) {
        return periodicTaskMap.get(task);
    }
}

