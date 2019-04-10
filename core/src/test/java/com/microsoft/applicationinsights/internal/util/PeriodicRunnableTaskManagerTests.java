package com.microsoft.applicationinsights.internal.util;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;


@RunWith(JUnit4.class)
public class PeriodicRunnableTaskManagerTests {

    @BeforeClass
    public static void initialize() {
        PeriodicTaskManager.initializer(1);
    }

    @After
    public void cleanUp() {
        PeriodicTaskManager.INSTANCE.stopAndClear();
    }

    @Test
    public void testNewTaskCanBeSubmittedAndIsRunning() {
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        ScheduledFuture<?> future = PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask);
        assertThat(future.isCancelled(), is(false));
        assertThat(PeriodicTaskManager.INSTANCE.getTask(periodicRunnableTask), notNullValue());
    }

    @Test
    public void testMultipleTaskCanBeSubmittedAndAreRunning() {
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask2 = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test1");
        ScheduledFuture<?> future = PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask);
        ScheduledFuture<?> future1 = PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask2);
        assertThat(future.isCancelled(), is(false));
        assertThat(PeriodicTaskManager.INSTANCE.getTask(periodicRunnableTask), notNullValue());
        assertThat(future1.isCancelled(), is(false));
        assertThat(PeriodicTaskManager.INSTANCE.getTask(periodicRunnableTask2), notNullValue());
    }

    @Test(expected = IllegalStateException.class)
    public void multipleTasksWithSameIdCannotBeSubmitted() {
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask2 = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        ScheduledFuture<?> future = PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask);
        ScheduledFuture<?> future1 = PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask2);
    }

    @Test
    public void canCancelSubmittedTask() {
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        ScheduledFuture<?> future = PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask);
        assertThat(future.isCancelled(), is(false));
        assertThat(PeriodicTaskManager.INSTANCE.getTask(periodicRunnableTask), notNullValue());

        PeriodicTaskManager.INSTANCE.cancelPeriodicTask(periodicRunnableTask);
        assertThat(future.isCancelled(), is(true));
        assertThat(PeriodicTaskManager.INSTANCE.getTask(periodicRunnableTask), nullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotScheduleNullRunnable() {
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskManager.PeriodicRunnableTask.getInstance(null, 0,
                1, TimeUnit.SECONDS, "Test");
        PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNegativeInitialDelay() {
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(), -1,
                1, TimeUnit.SECONDS, "Test");
        PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNegativeRepeatDuration() {
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(), 0,
                -1, TimeUnit.SECONDS, "Test");
        PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNullTaskId() {
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(), 0,
                1, TimeUnit.SECONDS, null);
        PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveEmptyTaskId() {
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(), 0,
                1, TimeUnit.SECONDS, "");
        PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNullTimeUnit() {
        PeriodicTaskManager.PeriodicRunnableTask periodicRunnableTask = PeriodicTaskManager.PeriodicRunnableTask.getInstance(new TestRunnable(), 0,
                1, null, "Test");
        PeriodicTaskManager.INSTANCE.executePeriodicRunnableTask(periodicRunnableTask);
    }

    private class TestRunnable implements Runnable {

        @Override
        public void run() {
            System.out.println("Hello....");
        }
    }
}
