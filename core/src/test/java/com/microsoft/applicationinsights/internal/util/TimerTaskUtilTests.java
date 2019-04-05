package com.microsoft.applicationinsights.internal.util;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;


@RunWith(JUnit4.class)
public class TimerTaskUtilTests {

    @After
    public void cleanUp() {
        TimerTaskUtil.reset();
    }

    @Test
    public void testNewTaskCanBeSubmittedAndIsRunning() {
        TimerTaskUtil.executePeriodicTask(new TestRunnable(), 0, 1, TimeUnit.SECONDS, TimerTaskUtil.class,
                "Test");
        ScheduledExecutorService service = TimerTaskUtil.getServiceTaskName("Test");
        assertThat(service.isShutdown(), is(false));
        assertThat(service.isTerminated(), is(false));
        assertThat(TimerTaskUtil.getServiceTaskName("Test"), notNullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotScheduleNullRunnable() {
        TimerTaskUtil.executePeriodicTask(null, 0, 1, TimeUnit.SECONDS, TimerTaskUtil.class,
                "Test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNegativeInitialDelay() {
        TimerTaskUtil.executePeriodicTask(new TestRunnable(), -1, 1, TimeUnit.SECONDS, TimerTaskUtil.class,
                "Test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNegativeRepeatDuration() {
        TimerTaskUtil.executePeriodicTask(new TestRunnable(), 0, -1, TimeUnit.SECONDS, TimerTaskUtil.class,
                "Test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNullTaskId() {
        TimerTaskUtil.executePeriodicTask(new TestRunnable(), 0, 1, TimeUnit.SECONDS, TimerTaskUtil.class,
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveEmptyTaskId() {
        TimerTaskUtil.executePeriodicTask(new TestRunnable(), 0, 1, TimeUnit.SECONDS, TimerTaskUtil.class,
                "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotHaveNullTimeUnit() {
        TimerTaskUtil.executePeriodicTask(new TestRunnable(), 0, 1, null, TimerTaskUtil.class,
                "Test");
    }

    private class TestRunnable implements Runnable {

        @Override
        public void run() {
            System.out.println("Hello....");
        }
    }
}
