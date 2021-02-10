package com.microsoft.applicationinsights.internal.util;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.failureCounter;
import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.previousFailureCounter;
import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.previousSuccessCounter;
import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.successCounter;
import static org.junit.Assert.*;

public class NextworkExceptionsTrackerTest {
    private static final ScheduledExecutorService networkIssueTrackerTest =
            Executors.newSingleThreadScheduledExecutor(
                    ThreadPoolUtils.createDaemonThreadFactory(
                            NetworkExceptionsTracker.class,
                            "networkIssueTrackerTest"));

    @Before
    public void setUp() {
        // one-time initialization code
        networkIssueTrackerTest.scheduleAtFixedRate(new NetworkExceptionsTracker(), 0, 1, TimeUnit.MILLISECONDS);
    }

    @After
    public void tearDown() throws InterruptedException {
        // one-time cleanup code
        if (!networkIssueTrackerTest.isShutdown()) {
            networkIssueTrackerTest.shutdown();
        }
        networkIssueTrackerTest.awaitTermination(2, TimeUnit.SECONDS);
    }


    @Test
    public void testPreviousCountersAreAssigned() throws InterruptedException {
        Timer timer = new Timer("Timer");
        TimerTask incrementFailureTask = new TimerTask() {
            public void run() {
                try {
                    failureCounter.incrementAndGet();
                    Thread.sleep(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        TimerTask incrementSuccessTask = new TimerTask() {
            public void run() {
                try {
                    Thread.sleep(0);
                    successCounter.incrementAndGet();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };
        Thread failureThread = new Thread(incrementFailureTask);
        Thread successThread = new Thread(incrementSuccessTask);
        failureThread.start();
        successThread.start();
        assertEquals(0, previousFailureCounter.get());
        assertEquals(0, previousSuccessCounter.get());
        failureThread.join();
        successThread.join();
        assertEquals(1, previousFailureCounter.get());
        assertEquals(1, previousSuccessCounter.get());
        assertEquals(0, successCounter.get());
        assertEquals(0, failureCounter.get());
    }
}
