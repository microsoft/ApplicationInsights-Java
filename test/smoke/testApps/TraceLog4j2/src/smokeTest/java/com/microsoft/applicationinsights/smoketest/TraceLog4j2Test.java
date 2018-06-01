package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;

import org.junit.Test;

public class TraceLog4j2Test extends AiSmokeTest {

    @Test
    @TargetUri("/traceLog4j2")
    public void testTraceLog4j2() {
        assertEquals(6, mockedIngestion.getCountForType("MessageData"));

        MessageData md1 = getTelemetryDataForType(0, "MessageData");
        assertEquals("This is log4j2 trace.", md1.getMessage());
        assertEquals(SeverityLevel.Verbose, md1.getSeverityLevel());
        assertEquals("Log4j", md1.getProperties().get("SourceType"));
        assertEquals("TRACE", md1.getProperties().get("LoggingLevel"));

        MessageData md2 = getTelemetryDataForType(1, "MessageData");
        assertEquals("This is log4j2 debug.", md2.getMessage());
        assertEquals(SeverityLevel.Verbose, md2.getSeverityLevel());
        assertEquals("Log4j", md2.getProperties().get("SourceType"));
        assertEquals("DEBUG", md2.getProperties().get("LoggingLevel"));

        MessageData md3 = getTelemetryDataForType(2, "MessageData");
        assertEquals("This is log4j2 info.", md3.getMessage());
        assertEquals(SeverityLevel.Information, md3.getSeverityLevel());
        assertEquals("Log4j", md3.getProperties().get("SourceType"));
        assertEquals("INFO", md3.getProperties().get("LoggingLevel"));

        MessageData md4 = getTelemetryDataForType(3, "MessageData");
        assertEquals("This is log4j2 warn.", md4.getMessage());
        assertEquals(SeverityLevel.Warning, md4.getSeverityLevel());
        assertEquals("Log4j", md4.getProperties().get("SourceType"));
        assertEquals("WARN", md4.getProperties().get("LoggingLevel"));


        MessageData md5 = getTelemetryDataForType(4, "MessageData");
        assertEquals("This is log4j2 error.", md5.getMessage());
        assertEquals(SeverityLevel.Error, md5.getSeverityLevel());
        assertEquals("Log4j", md5.getProperties().get("SourceType"));
        assertEquals("ERROR", md5.getProperties().get("LoggingLevel"));

        MessageData md6 = getTelemetryDataForType(5, "MessageData");
        assertEquals("This is log4j2 fatal.", md6.getMessage());
        assertEquals(SeverityLevel.Critical, md6.getSeverityLevel());
        assertEquals("Log4j", md6.getProperties().get("SourceType"));
        assertEquals("FATAL", md6.getProperties().get("LoggingLevel"));
    }

    @Test
    @TargetUri("/traceLog4j2WithException")
    public void testTraceLog4j2WithException() {
        assertEquals(1, mockedIngestion.getCountForType("ExceptionData"));

        ExceptionData ed1 = getTelemetryDataForType(0, "ExceptionData");
        List<ExceptionDetails> details = ed1.getExceptions();
        ExceptionDetails ex = details.get(0);

        assertEquals("Fake Exception", ex.getMessage());
        assertEquals(SeverityLevel.Error, ed1.getSeverityLevel());
        assertEquals("This is an exception!", ed1.getProperties().get("Logger Message"));
        assertEquals("Log4j", ed1.getProperties().get("SourceType"));
        assertEquals("ERROR", ed1.getProperties().get("LoggingLevel"));
    }
}