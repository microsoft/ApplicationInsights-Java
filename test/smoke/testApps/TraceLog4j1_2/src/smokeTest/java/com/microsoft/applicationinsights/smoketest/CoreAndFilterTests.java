package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;

import org.junit.Test;

public class CoreAndFilterTests extends AiSmokeTest {
    @Test
    @TargetUri("/traceLog4j1_2")
    public void testTraceLog4j1_2() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("MessageData"));

        MessageData md1 = getTelemetryDataForType(0, "MessageData");
        assertEquals("This is log4j1.2 trace.", md1.getMessage());
        assertEquals(SeverityLevel.Verbose, md1.getSeverityLevel());
        assertEquals("Log4j", md1.getProperties().get("SourceType"));
        assertEquals("TRACE", md1.getProperties().get("LoggingLevel"));
    }
}