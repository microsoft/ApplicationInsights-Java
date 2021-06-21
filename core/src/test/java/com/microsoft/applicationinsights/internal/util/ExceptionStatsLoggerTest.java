package com.microsoft.applicationinsights.internal.util;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionStatsLoggerTest {
    private static ExceptionStats networkExceptionStats;

    @BeforeAll
    public static void setUp() {
        // one-time initialization code
        networkExceptionStats = new ExceptionStats(ExceptionStatsLoggerTest.class, "intro:", 1);
    }

    @Test
    public void testWarnAndExceptionsAreLogged() throws InterruptedException {
        LogCaptor logCaptor = LogCaptor.forClass(ExceptionStatsLoggerTest.class);
        networkExceptionStats.recordSuccess();
        Exception ex=new IllegalArgumentException();
        networkExceptionStats.recordFailure("Test Message",ex);
        networkExceptionStats.recordFailure("Test Message2",ex);
        networkExceptionStats.recordFailure("Test Message2",ex);
        networkExceptionStats.recordFailure("Test Message3",ex);
        //wait for more than 1 second
        Thread.sleep(3000);

        assertThat(logCaptor.getWarnLogs()).hasSize(2);
        assertThat(logCaptor.getWarnLogs().get(0)).contains("intro: Test Message (future failures will be aggregated and logged once every 0 minutes)");
        assertThat(logCaptor.getWarnLogs().get(1)).contains("In the last 0 minutes, the following operation has failed 3 times (out of 4):\n" +
                "intro:\n" +
                " * Test Message2 (2 times)\n" +
                " * Test Message3 (1 times)");
    }
}
