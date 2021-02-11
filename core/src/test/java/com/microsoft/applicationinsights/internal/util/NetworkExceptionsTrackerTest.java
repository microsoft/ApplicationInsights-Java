package com.microsoft.applicationinsights.internal.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nl.altindag.log.LogCaptor;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.applicationinsights.common.CommonUtils.failureCounter;
import static com.microsoft.applicationinsights.common.CommonUtils.handleTemporaryExceptions;
import static com.microsoft.applicationinsights.common.CommonUtils.lastTemporaryException;
import static com.microsoft.applicationinsights.common.CommonUtils.lastTemporaryExceptionLogger;
import static com.microsoft.applicationinsights.common.CommonUtils.lastTemporaryExceptionMessage;
import static com.microsoft.applicationinsights.common.CommonUtils.successCounter;
import static org.junit.Assert.*;

public class NetworkExceptionsTrackerTest {
    private static final ScheduledExecutorService networkIssueTrackerTest =
            Executors.newSingleThreadScheduledExecutor(
                    ThreadPoolUtils.createDaemonThreadFactory(
                            NetworkExceptionsTracker.class,
                            "networkIssueTrackerTest"));

    @BeforeClass
    public static void setUp() {
        // one-time initialization code
        networkIssueTrackerTest.scheduleAtFixedRate(new NetworkExceptionsTracker(), 0, 1, TimeUnit.MILLISECONDS);
    }

    @After
    public void tearDown() throws InterruptedException {
        // cleanup code
        successCounter.set(0);
        failureCounter.set(0);
        lastTemporaryException.set(null);
        lastTemporaryExceptionLogger.set(null);
        lastTemporaryExceptionMessage.set(null);
    }

    @AfterClass
    public static void cleanup() throws InterruptedException {
        if (!networkIssueTrackerTest.isShutdown()) {
            networkIssueTrackerTest.shutdown();
        }
        networkIssueTrackerTest.awaitTermination(2, TimeUnit.SECONDS);
    }


    @Test
    public void testSuccessAndFailureCounters() throws InterruptedException {
        assertEquals(0, failureCounter.get());
        assertEquals(0, successCounter.get());
        successCounter.getAndIncrement();
        assertEquals(1, successCounter.get());
        Logger logger = LoggerFactory.getLogger(NetworkExceptionsTrackerTest.class);
        Exception ex=new IllegalArgumentException();
        handleTemporaryExceptions(logger,"Test Message",ex);
        assertEquals(1, failureCounter.get());
        assertEquals(ex,lastTemporaryException.get());
        assertEquals("Test Message",lastTemporaryExceptionMessage.get());
        assertEquals(logger,lastTemporaryExceptionLogger.get());
    }

    @Test
    public void testFailureCountersAfterSingleFailure() throws InterruptedException {
        assertEquals(0, failureCounter.get());
        assertEquals(0, successCounter.get());
        successCounter.getAndIncrement();
        failureCounter.getAndIncrement();
        assertEquals(1, successCounter.get());
        assertEquals(1, failureCounter.get());
        Logger logger = LoggerFactory.getLogger(NetworkExceptionsTrackerTest.class);
        Exception ex=new IllegalArgumentException();
        handleTemporaryExceptions(logger,"Test Message",ex);
        assertEquals(2, failureCounter.get());
        assertNull(lastTemporaryException.get());
        assertNull(lastTemporaryExceptionMessage.get());
        assertNull(lastTemporaryExceptionLogger.get());
    }

    @Test
    public void testExceptionLogged() {
        LogCaptor logCaptor = LogCaptor.forClass(NetworkExceptionsTrackerTest.class);
        assertEquals(0, failureCounter.get());
        assertEquals(0, successCounter.get());
        successCounter.getAndIncrement();
        assertEquals(1, successCounter.get());
        Logger logger = LoggerFactory.getLogger(NetworkExceptionsTrackerTest.class);
        Exception ex=new IllegalArgumentException();
        handleTemporaryExceptions(logger,"Test Message",ex);
        assertEquals(1, failureCounter.get());
        assertEquals(ex,lastTemporaryException.get());
        assertEquals("Test Message",lastTemporaryExceptionMessage.get());
        assertEquals(logger,lastTemporaryExceptionLogger.get());
        long start = System.currentTimeMillis();
        long end = start + 3000;
        while (System.currentTimeMillis() < end) {
            // Wait for 3 secs, so that NetworkExceptionsTracker logs the exception
        }
        assertEquals(1,logCaptor.getErrorLogs().size());
    }

}
