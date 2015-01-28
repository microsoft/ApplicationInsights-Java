package com.microsoft.applicationinsights.internal.log4j.v1_2;

import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ApplicationInsightsLogEventTest {
    @Test
    public void testFatalSeverityLevel() {
        testSeverityLevel(Level.FATAL, SeverityLevel.Critical);
    }

    @Test
    public void testErrorSeverityLevel() {
        testSeverityLevel(Level.ERROR, SeverityLevel.Error);
    }

    @Test
    public void testInfoSeverityLevel() {
        testSeverityLevel(Level.INFO, SeverityLevel.Information);
    }

    @Test
    public void testWarningSeverityLevel() {
        testSeverityLevel(Level.WARN, SeverityLevel.Warning);
    }

    @Test
    public void testDebugSeverityLevel() {
        testSeverityLevel(Level.DEBUG, SeverityLevel.Verbose);
    }

    @Test
    public void testTraceSeverityLevel() {
        testSeverityLevel(Level.TRACE, SeverityLevel.Verbose);
    }

    @Test
    public void testAllSeverityLevel() {
        testSeverityLevel(Level.ALL, SeverityLevel.Verbose);
    }

    private static void testSeverityLevel(Level level, SeverityLevel expected) {
        LoggingEvent loggingEvent = new LoggingEvent("mockClass", Logger.getLogger("com.microsoft.applicationinsights.internal.log4j.v1_2.ApplicationInsightsLogEventTest"), 0L, level, "MockMessage", null);
        ApplicationInsightsLogEvent event = new ApplicationInsightsLogEvent(loggingEvent);

        assertEquals(expected, event.getSeverityLevel());
    }
}
