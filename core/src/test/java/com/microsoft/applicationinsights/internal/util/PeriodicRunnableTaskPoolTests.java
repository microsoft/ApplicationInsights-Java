package com.microsoft.applicationinsights.internal.util;

import org.junit.jupiter.api.*;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PeriodicRunnableTaskPoolTests {

    private PeriodicTaskPool taskPool = null;

    @BeforeEach
    public void initialize() {
        taskPool = new PeriodicTaskPool(1, "test-pool");
    }

    @AfterEach
    public void cleanUp() {
        taskPool.stop(5, TimeUnit.SECONDS);
        taskPool = null;
    }

    @Test
    public void testNewTaskCanBeSubmittedAndIsRunning() {
        SignalListener signal = new SignalListener();
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new TestRunnable(signal),
                0, 1, TimeUnit.SECONDS, "Test");
        ScheduledFuture<?> future = taskPool.executePeriodicRunnableTask(periodicRunnableTask);
        assertThat(future.isCancelled()).isFalse();
        assertThat((Future<?>) taskPool.getTask(periodicRunnableTask)).isNotNull();
        sleep(1, TimeUnit.SECONDS);
        assertThat(signal.isDone()).isTrue();
    }

    @Test
    public void testMultipleTaskCanBeSubmittedAndAreRunning() {
        SignalListener sig1 = new SignalListener();
        SignalListener sig2 = new SignalListener();
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new TestRunnable(sig1),
                0, 1, TimeUnit.SECONDS, "Test");
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask2 = PeriodicTaskPool.PeriodicRunnableTask.createTask(new TestRunnable(sig2),
                0, 1, TimeUnit.SECONDS, "Test1");
        ScheduledFuture<?> future = taskPool.executePeriodicRunnableTask(periodicRunnableTask);
        ScheduledFuture<?> future1 = taskPool.executePeriodicRunnableTask(periodicRunnableTask2);
        assertThat(future.isCancelled()).isFalse();
        assertThat((Future<?>) taskPool.getTask(periodicRunnableTask)).isNotNull();
        assertThat(future1.isCancelled()).isFalse();
        assertThat((Future<?>) taskPool.getTask(periodicRunnableTask2)).isNotNull();

        sleep(1, TimeUnit.SECONDS);
        assertThat(sig1.isDone()).isTrue();
        assertThat(sig2.isDone()).isTrue();
    }

    @Test
    public void multipleTasksWithSameIdCannotBeSubmitted() {
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask2 = PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        taskPool.executePeriodicRunnableTask(periodicRunnableTask);

        assertThatThrownBy(() -> taskPool.executePeriodicRunnableTask(periodicRunnableTask2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void canCancelSubmittedTask() {
        SignalListener signal = new SignalListener();
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new TestRunnable(signal),
                0, 10, TimeUnit.SECONDS, "Test");
        ScheduledFuture<?> future = taskPool.executePeriodicRunnableTask(periodicRunnableTask);
        assertThat(future.isCancelled()).isFalse();
        assertThat((Future<?>) taskPool.getTask(periodicRunnableTask)).isNotNull();

        taskPool.cancelPeriodicTask(periodicRunnableTask);
        assertThat(future.isCancelled()).isTrue();
        assertThat((Future<?>) taskPool.getTask(periodicRunnableTask)).isNull();

        sleep(10, TimeUnit.SECONDS);
        assertThat(signal.isDone()).isFalse();
    }

    @Test
    public void cannotScheduleNullRunnable() {
        assertThatThrownBy(() ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(null, 0,1, TimeUnit.SECONDS, "Test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void cannotHaveNegativeInitialDelay() {
        assertThatThrownBy(() ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(), -1,1, TimeUnit.SECONDS, "Test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void cannotHaveNegativeRepeatDuration() {
        assertThatThrownBy(() ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(), 0, -1, TimeUnit.SECONDS, "Test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void cannotHaveNullTaskId() {
        assertThatThrownBy(() ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(), 0,1, TimeUnit.SECONDS, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void cannotHaveEmptyTaskId() {
        assertThatThrownBy(() ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(), 0,1, TimeUnit.SECONDS, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void cannotHaveNullTimeUnit() {
        assertThatThrownBy(() ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(), 0,1, null, "Test"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void sleep(int delay, TimeUnit unit) {
        try {
            unit.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private interface CompletionListener {
        void complete();
    }

    private static class SignalListener implements CompletionListener {
        private volatile boolean done = false;

        @Override
        public void complete() {
            synchronized (this) {
                done = true;
            }
        }

        public synchronized boolean isDone() {
            return done;
        }
    }

    private static class DoNothingRunnable implements Runnable {
        @Override
        public void run() {

        }
    }

    private static class TestRunnable implements Runnable {
        private final CompletionListener listener;

        public TestRunnable(CompletionListener listener) {
            this.listener = listener;
        }

        @Override
        public void run() {
            System.out.println("Hello....");
            listener.complete();
        }
    }
}
