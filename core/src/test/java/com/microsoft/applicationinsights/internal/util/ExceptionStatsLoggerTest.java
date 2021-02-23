package com.microsoft.applicationinsights.internal.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nl.altindag.log.LogCaptor;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.networkExceptionStats;
import static org.junit.Assert.*;

public class ExceptionStatsLoggerTest {
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
        networkExceptionStats.set(new ExceptionStats());
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
        ExceptionStats exceptionStats = networkExceptionStats.get();
        assertEquals(0L, exceptionStats.getNumFailures());
        assertEquals(0L, exceptionStats.getNumSuccesses());
        exceptionStats.incrementSuccessCounter();
        assertEquals(1L, exceptionStats.getNumSuccesses());
        Logger logger = LoggerFactory.getLogger(ExceptionStatsLoggerTest.class);
        Exception ex=new IllegalArgumentException();
        networkExceptionStats.set(new ExceptionStats(networkExceptionStats.get().getSuccessCounter(),
                networkExceptionStats.get().getFailureCounter()+1, ex, logger, "Test Message"));
        assertEquals(1L, networkExceptionStats.get().getFailureCounter());
        assertEquals(ex, networkExceptionStats.get().getException());
        assertEquals("Test Message", networkExceptionStats.get().getWarningMessage());
        assertEquals(logger, networkExceptionStats.get().getLogger());
    }

    @Test
    public void testExceptionLogged() throws InterruptedException {
        LogCaptor logCaptor = LogCaptor.forClass(ExceptionStatsLoggerTest.class);
        ExceptionStats exceptionStats = networkExceptionStats.get();
        assertEquals(0L, exceptionStats.getNumFailures());
        assertEquals(0L, exceptionStats.getNumSuccesses());
        exceptionStats.incrementSuccessCounter();
        assertEquals(1L, exceptionStats.getNumSuccesses());
        Logger logger = LoggerFactory.getLogger(ExceptionStatsLoggerTest.class);
        Exception ex=new IllegalArgumentException();
        networkExceptionStats.set(new ExceptionStats(networkExceptionStats.get().getSuccessCounter(),
                networkExceptionStats.get().getFailureCounter()+1, ex, logger, "Test Message"));
        assertEquals(1L, networkExceptionStats.get().getFailureCounter());
        assertEquals(ex, networkExceptionStats.get().getException());
        assertEquals("Test Message", networkExceptionStats.get().getWarningMessage());
        assertEquals(logger, networkExceptionStats.get().getLogger());
        //wait for 3 secs
        Thread.sleep(3000);
        assertEquals(1,logCaptor.getErrorLogs().size());
    }

}
