package com.microsoft.applicationinsights.internal.util;

import org.junit.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;


public class PeriodicRunnableTaskPoolTests {

    private PeriodicTaskPool taskPool = null;

    @Before
    public void initialize() {
        taskPool = new PeriodicTaskPool(1, "test-pool");
    }

    @After
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
        assertThat(future.isCancelled(), is(false));
        assertThat(taskPool.getTask(periodicRunnableTask), notNullValue());
        sleep(1, TimeUnit.SECONDS);
        assertTrue("Did not receive signal from Runnable", signal.isDone());
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
        assertThat(future.isCancelled(), is(false));
        assertThat(taskPool.getTask(periodicRunnableTask), notNullValue());
        assertThat(future1.isCancelled(), is(false));
        assertThat(taskPool.getTask(periodicRunnableTask2), notNullValue());

        sleep(1, TimeUnit.SECONDS);
        assertTrue("'Test' runnable did not signal", sig1.isDone());
        assertTrue("'Test1' runnable did not signal", sig2.isDone());
    }

    @Test(expected = IllegalStateException.class)
    public void multipleTasksWithSameIdCannotBeSubmitted() {
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask2 = PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        taskPool.executePeriodicRunnableTask(periodicRunnableTask);
        taskPool.executePeriodicRunnableTask(periodicRunnableTask2);
    }

    @Test
    public void canCancelSubmittedTask() {
        final SignalListener signal = new SignalListener();
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new TestRunnable(signal),
                0, 10, TimeUnit.SECONDS, "Test");
        ScheduledFuture<?> future = taskPool.executePeriodicRunnableTask(periodicRunnableTask);
        assertThat(future.isCancelled(), is(false));
        assertThat(taskPool.getTask(periodicRunnableTask), notNullValue());

        taskPool.cancelPeriodicTask(periodicRunnableTask);
        assertThat(future.isCancelled(), is(true));
        assertThat(taskPool.getTask(periodicRunnableTask), nullValue());

        sleep(10, TimeUnit.SECONDS);
        assertFalse("Runnable executed after cancellation", signal.isDone());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotScheduleNullRunnable() {
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(null, 0,
                1, TimeUnit.SECONDS, "Test");
        taskPool.executePeriodicRunnableTask(periodicRunnableTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNegativeInitialDelay() {
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(), -1,
                1, TimeUnit.SECONDS, "Test");
        taskPool.executePeriodicRunnableTask(periodicRunnableTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNegativeRepeatDuration() {
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(), 0,
                -1, TimeUnit.SECONDS, "Test");
        taskPool.executePeriodicRunnableTask(periodicRunnableTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNullTaskId() {
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(), 0,
                1, TimeUnit.SECONDS, null);
        taskPool.executePeriodicRunnableTask(periodicRunnableTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveEmptyTaskId() {
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(), 0,
                1, TimeUnit.SECONDS, "");
        taskPool.executePeriodicRunnableTask(periodicRunnableTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNullTimeUnit() {
        PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskPool.PeriodicRunnableTask.createTask(new DoNothingRunnable(), 0,
                1, null, "Test");
        taskPool.executePeriodicRunnableTask(periodicRunnableTask);
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

    private class DoNothingRunnable implements Runnable {
        @Override
        public void run() {

        }
    }

    private class TestRunnable implements Runnable {
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
