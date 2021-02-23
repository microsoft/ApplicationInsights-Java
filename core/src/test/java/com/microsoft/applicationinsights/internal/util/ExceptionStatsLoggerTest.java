package com.microsoft.applicationinsights.internal.util;

import nl.altindag.log.LogCaptor;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class ExceptionStatsLoggerTest {
    private static ExceptionStats networkExceptionStats;
    private static Logger logger;

    @BeforeClass
    public static void setUp() {
        // one-time initialization code
        networkExceptionStats = new ExceptionStats(0,1);
        logger = LoggerFactory.getLogger(ExceptionStatsLoggerTest.class);
    }

    @Test
    public void testWarnAndExceptionLogged() throws InterruptedException {
        LogCaptor logCaptor = LogCaptor.forClass(ExceptionStatsLoggerTest.class);
        networkExceptionStats.recordSuccess();
        Exception ex=new IllegalArgumentException();
        networkExceptionStats.recordException("Test Message",ex,logger);
        networkExceptionStats.recordException("Test Message2",ex,logger);
        //wait for 3 secs
        Thread.sleep(3000);
        assertEquals(1,logCaptor.getErrorLogs().size());
        assertEquals(1,logCaptor.getWarnLogs().size());
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Total number of failed telemetry requests in the last 5 minutes: 1"));
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Total number of successful telemetry requests in the last 5 minutes: 1"));
    }

}
