package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

@UseAgent
public class TraceLogBackTest extends AiSmokeTest {

    @Before
    public void skipJbosseap6AndJbosseap7Image() {
        // this doesn't work with jbosseap6 and jbosseap7;
        Assume.assumeFalse(currentImageName.contains("jbosseap6"));
        Assume.assumeFalse(currentImageName.contains("jbosseap7"));
    }

    @Test
    @TargetUri("/traceLogBack")
    public void testTraceLogBack() {
        assertEquals(2, mockedIngestion.getCountForType("MessageData"));

        MessageData md1 = getTelemetryDataForType(0, "MessageData");
        assertEquals("This is logback warn.", md1.getMessage());
        assertEquals(SeverityLevel.Warning, md1.getSeverityLevel());
        assertEquals("Logger", md1.getProperties().get("SourceType"));
        assertEquals("WARN", md1.getProperties().get("LoggingLevel"));

        MessageData md2 = getTelemetryDataForType(1, "MessageData");
        assertEquals("This is logback error.", md2.getMessage());
        assertEquals(SeverityLevel.Error, md2.getSeverityLevel());
        assertEquals("Logger", md2.getProperties().get("SourceType"));
        assertEquals("ERROR", md2.getProperties().get("LoggingLevel"));
    }

    @Test
    @TargetUri("traceLogBackWithException")
    public void testTraceLogBackWithExeption() {
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