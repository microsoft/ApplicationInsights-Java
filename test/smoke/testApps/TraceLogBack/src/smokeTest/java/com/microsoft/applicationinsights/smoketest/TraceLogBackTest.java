package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import java.util.Comparator;
import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import org.junit.Test;

@RequestCapturing(enabled = false)
public class TraceLogBackTest extends AiSmokeTest {

    @Test
    @TargetUri("/traceLogBack")
    public void testTraceLogBack() throws Exception {
        mockedIngestion.waitForItems("MessageData", 5);

        final List<MessageData> logs = mockedIngestion.getTelemetryDataByType("MessageData");
        logs.sort(new Comparator<MessageData>() {
            @Override
            public int compare(MessageData o1, MessageData o2) {
                final int i = o1.getSeverityLevel().compareTo(o2.getSeverityLevel());
                if (i == 0) {
                    return o1.getProperties().get("LoggingLevel").compareTo(o2.getProperties().get("LoggingLevel"));
                }
                return i;
            }
        });

        MessageData md1 = logs.get(1);
        assertEquals("This is logback trace.", md1.getMessage());
        assertEquals(SeverityLevel.Verbose, md1.getSeverityLevel());
        assertEquals("LOGBack", md1.getProperties().get("SourceType"));
        assertEquals("TRACE", md1.getProperties().get("LoggingLevel"));

        MessageData md2 = logs.get(0);
        assertEquals("This is logback debug.", md2.getMessage());
        assertEquals(SeverityLevel.Verbose, md2.getSeverityLevel());
        assertEquals("LOGBack", md2.getProperties().get("SourceType"));
        assertEquals("DEBUG", md2.getProperties().get("LoggingLevel"));

        MessageData md3 = logs.get(2);
        assertEquals("This is logback info.", md3.getMessage());
        assertEquals(SeverityLevel.Information, md3.getSeverityLevel());
        assertEquals("LOGBack", md3.getProperties().get("SourceType"));
        assertEquals("INFO", md3.getProperties().get("LoggingLevel"));

        MessageData md4 = logs.get(3);
        assertEquals("This is logback warn.", md4.getMessage());
        assertEquals(SeverityLevel.Warning, md4.getSeverityLevel());
        assertEquals("LOGBack", md4.getProperties().get("SourceType"));
        assertEquals("WARN", md4.getProperties().get("LoggingLevel"));

        MessageData md5 = logs.get(4);
        assertEquals("This is logback error.", md5.getMessage());
        assertEquals(SeverityLevel.Error, md5.getSeverityLevel());
        assertEquals("LOGBack", md5.getProperties().get("SourceType"));
        assertEquals("ERROR", md5.getProperties().get("LoggingLevel"));
    }

    @Test
    @TargetUri("traceLogBackWithException")
    public void testTraceLogBackWithExeption() throws Exception {
        mockedIngestion.waitForItems("ExceptionData", 1);

        ExceptionData ed1 = getTelemetryDataForType(0, "ExceptionData");
        List<ExceptionDetails> details = ed1.getExceptions();
        ExceptionDetails ex = details.get(0);

        assertEquals("Fake Exception", ex.getMessage());
        assertEquals(SeverityLevel.Error, ed1.getSeverityLevel());
        assertEquals("This is an exception!", ed1.getProperties().get("Logger Message"));
        assertEquals("LOGBack", ed1.getProperties().get("SourceType"));
        assertEquals("ERROR", ed1.getProperties().get("LoggingLevel"));
    }
}