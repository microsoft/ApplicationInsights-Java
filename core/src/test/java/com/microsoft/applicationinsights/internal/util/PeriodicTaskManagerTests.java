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
public class PeriodicTaskManagerTests {

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
        PeriodicTaskManager.PeriodicTask periodicTask = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        ScheduledFuture<?> future = PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask);
        assertThat(future.isCancelled(), is(false));
        assertThat(PeriodicTaskManager.INSTANCE.getTask(periodicTask), notNullValue());
    }

    @Test
    public void testMultipleTaskCanBeSubmittedAndAreRunning() {
        PeriodicTaskManager.PeriodicTask periodicTask = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        PeriodicTaskManager.PeriodicTask periodicTask2 = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test1");
        ScheduledFuture<?> future = PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask);
        ScheduledFuture<?> future1 = PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask2);
        assertThat(future.isCancelled(), is(false));
        assertThat(PeriodicTaskManager.INSTANCE.getTask(periodicTask), notNullValue());
        assertThat(future1.isCancelled(), is(false));
        assertThat(PeriodicTaskManager.INSTANCE.getTask(periodicTask2), notNullValue());
    }

    @Test(expected = IllegalStateException.class)
    public void multipleTasksWithSameIdCannotBeSubmitted() {
        PeriodicTaskManager.PeriodicTask periodicTask = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        PeriodicTaskManager.PeriodicTask periodicTask2 = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        ScheduledFuture<?> future = PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask);
        ScheduledFuture<?> future1 = PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask2);
    }

    @Test
    public void canCancelSubmittedTask() {
        PeriodicTaskManager.PeriodicTask periodicTask = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(),
                0, 1, TimeUnit.SECONDS, "Test");
        ScheduledFuture<?> future = PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask);
        assertThat(future.isCancelled(), is(false));
        assertThat(PeriodicTaskManager.INSTANCE.getTask(periodicTask), notNullValue());

        PeriodicTaskManager.INSTANCE.cancelPeriodicTask(periodicTask);
        assertThat(future.isCancelled(), is(true));
        assertThat(PeriodicTaskManager.INSTANCE.getTask(periodicTask), nullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotScheduleNullRunnable() {
        PeriodicTaskManager.PeriodicTask periodicTask = PeriodicTaskManager.PeriodicTask.getInstance(null, 0,
                1, TimeUnit.SECONDS, "Test");
        PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNegativeInitialDelay() {
        PeriodicTaskManager.PeriodicTask periodicTask = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(), -1,
                1, TimeUnit.SECONDS, "Test");
        PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNegativeRepeatDuration() {
        PeriodicTaskManager.PeriodicTask periodicTask = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(), 0,
                -1, TimeUnit.SECONDS, "Test");
        PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNullTaskId() {
        PeriodicTaskManager.PeriodicTask periodicTask = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(), 0,
                1, TimeUnit.SECONDS, null);
        PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveEmptyTaskId() {
        PeriodicTaskManager.PeriodicTask periodicTask = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(), 0,
                1, TimeUnit.SECONDS, "");
        PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNullTimeUnit() {
        PeriodicTaskManager.PeriodicTask periodicTask = PeriodicTaskManager.PeriodicTask.getInstance(new TestRunnable(), 0,
                1, null, "Test");
        PeriodicTaskManager.INSTANCE.executePeriodicTask(periodicTask);
    }

    private class TestRunnable implements Runnable {

        @Override
        public void run() {
            System.out.println("Hello....");
        }
    }
}
