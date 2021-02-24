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
        networkExceptionStats = new ExceptionStats(3);
        logger = LoggerFactory.getLogger(ExceptionStatsLoggerTest.class);
    }

    @Test
    public void testWarnAndExceptionsAreLogged() throws InterruptedException {
        LogCaptor logCaptorLocal = LogCaptor.forClass(ExceptionStatsLoggerTest.class);
        LogCaptor logCaptor = LogCaptor.forClass(ExceptionStats.class);
        networkExceptionStats.recordSuccess();
        Exception ex=new IllegalArgumentException();
        networkExceptionStats.recordException("Test Message",ex,logger);
        networkExceptionStats.recordException("Test Message2",ex,logger);
        networkExceptionStats.recordException("Test Message2",ex,logger);
        networkExceptionStats.recordException("Test Message3",ex,logger);
        //wait for 3 secs
        Thread.sleep(3000);
        assertEquals(2,logCaptor.getErrorLogs().size());
        assertEquals(1,logCaptor.getWarnLogs().size());
        System.out.println(logCaptor.getWarnLogs());
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Test Message2 (failed 2 times in the last 0 minutes)"));
        assertTrue(logCaptor.getErrorLogs().get(1).contains("Test Message3 (failed 1 times in the last 0 minutes)"));
        assertTrue(logCaptorLocal.getWarnLogs().get(0).contains("Test Message (future failures will be aggregated and logged once every 0 minutes)"));
        assertTrue(logCaptor.getWarnLogs().get(0).contains("3/4(Total Failures/Total Requests) reported in the last 0 minutes"));
    }

}
