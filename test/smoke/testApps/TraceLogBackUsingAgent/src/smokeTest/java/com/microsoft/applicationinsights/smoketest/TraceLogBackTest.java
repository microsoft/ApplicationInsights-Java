package com.microsoft.applicationinsights.smoketest;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
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
        assertEquals(5, mockedIngestion.getCountForType("MessageData"));

        MessageData md1 = getTelemetryDataForType(0, "MessageData");
        assertEquals("This is logback trace.", md1.getMessage());
        assertEquals(SeverityLevel.Verbose, md1.getSeverityLevel());
        assertEquals("Logger", md1.getProperties().get("SourceType"));
        assertEquals("TRACE", md1.getProperties().get("LoggingLevel"));
        validateSdkName(md1, "ja-logging");

        MessageData md2 = getTelemetryDataForType(1, "MessageData");
        assertEquals("This is logback debug.", md2.getMessage());
        assertEquals(SeverityLevel.Verbose, md2.getSeverityLevel());
        assertEquals("Logger", md2.getProperties().get("SourceType"));
        assertEquals("DEBUG", md2.getProperties().get("LoggingLevel"));
        validateSdkName(md2, "ja-logging");

        MessageData md3 = getTelemetryDataForType(2, "MessageData");
        assertEquals("This is logback info.", md3.getMessage());
        assertEquals(SeverityLevel.Information, md3.getSeverityLevel());
        assertEquals("Logger", md3.getProperties().get("SourceType"));
        assertEquals("INFO", md3.getProperties().get("LoggingLevel"));
        validateSdkName(md3, "ja-logging");

        MessageData md4 = getTelemetryDataForType(3, "MessageData");
        assertEquals("This is logback warn.", md4.getMessage());
        assertEquals(SeverityLevel.Warning, md4.getSeverityLevel());
        assertEquals("Logger", md4.getProperties().get("SourceType"));
        assertEquals("WARN", md4.getProperties().get("LoggingLevel"));
        validateSdkName(md4, "ja-logging");

        MessageData md5 = getTelemetryDataForType(4, "MessageData");
        assertEquals("This is logback error.", md5.getMessage());
        assertEquals(SeverityLevel.Error, md5.getSeverityLevel());
        assertEquals("Logger", md5.getProperties().get("SourceType"));
        assertEquals("ERROR", md5.getProperties().get("LoggingLevel"));
        validateSdkName(md5, "ja-logging");
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
        validateSdkName(ed1, "ja-logging");
    }

    private void validateSdkName(Domain data, String sdkName) {
        Envelope envelope = mockedIngestion.getEnvelopeForBaseData(data);
        String sdkVersion = envelope.getTags().get("ai.internal.sdkVersion");
        assertThat(sdkVersion, startsWith(sdkName + ":"));
    }
}