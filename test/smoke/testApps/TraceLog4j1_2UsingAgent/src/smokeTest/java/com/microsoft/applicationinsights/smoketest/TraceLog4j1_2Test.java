package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import org.junit.Test;

@UseAgent
public class TraceLog4j1_2Test extends AiSmokeTest {

    @Test
    @TargetUri("/traceLog4j1_2")
    public void testTraceLog4j1_2() {

        assertEquals(3, mockedIngestion.getCountForType("MessageData"));

        MessageData md1 = getTelemetryDataForType(0, "MessageData");
        assertEquals("This is log4j1.2 warn.", md1.getMessage());
        assertEquals(SeverityLevel.Warning, md1.getSeverityLevel());
        assertEquals("Logger", md1.getProperties().get("SourceType"));
        assertEquals("WARN", md1.getProperties().get("LoggingLevel"));


        MessageData md2 = getTelemetryDataForType(1, "MessageData");
        assertEquals("This is log4j1.2 error.", md2.getMessage());
        assertEquals(SeverityLevel.Error, md2.getSeverityLevel());
        assertEquals("Logger", md2.getProperties().get("SourceType"));
        assertEquals("ERROR", md2.getProperties().get("LoggingLevel"));

        MessageData md3 = getTelemetryDataForType(2, "MessageData");
        assertEquals("This is log4j1.2 fatal.", md3.getMessage());
        assertEquals(SeverityLevel.Critical, md3.getSeverityLevel());
        assertEquals("Logger", md3.getProperties().get("SourceType"));
        assertEquals("FATAL", md3.getProperties().get("LoggingLevel"));
    }

    @Test
    @TargetUri("traceLog4j1_2WithException")
    public void testTraceLog4j1_2WithExeption() {
        assertEquals(1, mockedIngestion.getCountForType("ExceptionData"));

        ExceptionData ed1 = getTelemetryDataForType(0, "ExceptionData");
        List<ExceptionDetails> details = ed1.getExceptions();
        ExceptionDetails ex = details.get(0);

        assertEquals("Fake Exception", ex.getMessage());
        assertEquals(SeverityLevel.Error, ed1.getSeverityLevel());
        assertEquals("This is an exception!", ed1.getProperties().get("Logger Message"));
        assertEquals("Logger", ed1.getProperties().get("SourceType"));
        assertEquals("ERROR", ed1.getProperties().get("LoggingLevel"));
    }
}