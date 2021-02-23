package com.microsoft.applicationinsights.internal.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nl.altindag.log.LogCaptor;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.temporaryNetworkException;
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
        temporaryNetworkException.set(new TemporaryExceptionWrapper());
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
        TemporaryExceptionWrapper temporaryExceptionWrapper = temporaryNetworkException.get();
        assertEquals(0L, temporaryExceptionWrapper.getFailureCounter());
        assertEquals(0L, temporaryExceptionWrapper.getSuccessCounter());
        temporaryExceptionWrapper.incrementSuccessCounter();
        assertEquals(1L, temporaryExceptionWrapper.getSuccessCounter());
        Logger logger = LoggerFactory.getLogger(NetworkExceptionsTrackerTest.class);
        Exception ex=new IllegalArgumentException();
        temporaryNetworkException.set(new TemporaryExceptionWrapper(temporaryNetworkException.get().getSuccessCounter(),
                temporaryNetworkException.get().getFailureCounter()+1, ex, logger, "Test Message"));
        assertEquals(1L, temporaryNetworkException.get().getFailureCounter());
        assertEquals(ex,temporaryNetworkException.get().getLastTemporaryException());
        assertEquals("Test Message",temporaryNetworkException.get().getLastTemporaryExceptionMessage());
        assertEquals(logger,temporaryNetworkException.get().getLastTemporaryExceptionLogger());
    }

    @Test
    public void testExceptionLogged() throws InterruptedException {
        LogCaptor logCaptor = LogCaptor.forClass(NetworkExceptionsTrackerTest.class);
        TemporaryExceptionWrapper temporaryExceptionWrapper = temporaryNetworkException.get();
        assertEquals(0L, temporaryExceptionWrapper.getFailureCounter());
        assertEquals(0L, temporaryExceptionWrapper.getSuccessCounter());
        temporaryExceptionWrapper.incrementSuccessCounter();
        assertEquals(1L, temporaryExceptionWrapper.getSuccessCounter());
        Logger logger = LoggerFactory.getLogger(NetworkExceptionsTrackerTest.class);
        Exception ex=new IllegalArgumentException();
        temporaryNetworkException.set(new TemporaryExceptionWrapper(temporaryNetworkException.get().getSuccessCounter(),
                temporaryNetworkException.get().getFailureCounter()+1, ex, logger, "Test Message"));
        assertEquals(1L, temporaryNetworkException.get().getFailureCounter());
        assertEquals(ex,temporaryNetworkException.get().getLastTemporaryException());
        assertEquals("Test Message",temporaryNetworkException.get().getLastTemporaryExceptionMessage());
        assertEquals(logger,temporaryNetworkException.get().getLastTemporaryExceptionLogger());
        //wait for 3 secs
        Thread.sleep(3000);
        assertEquals(1,logCaptor.getErrorLogs().size());
    }

}
