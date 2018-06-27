package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import java.util.List;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

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
    assertEquals("LOGBack", md1.getProperties().get("SourceType"));
    assertEquals("TRACE", md1.getProperties().get("LoggingLevel"));

    MessageData md2 = getTelemetryDataForType(1, "MessageData");
    assertEquals("This is logback debug.", md2.getMessage());
    assertEquals(SeverityLevel.Verbose, md2.getSeverityLevel());
    assertEquals("LOGBack", md2.getProperties().get("SourceType"));
    assertEquals("DEBUG", md2.getProperties().get("LoggingLevel"));

    MessageData md3 = getTelemetryDataForType(2, "MessageData");
    assertEquals("This is logback info.", md3.getMessage());
    assertEquals(SeverityLevel.Information, md3.getSeverityLevel());
    assertEquals("LOGBack", md3.getProperties().get("SourceType"));
    assertEquals("INFO", md3.getProperties().get("LoggingLevel"));

    MessageData md4 = getTelemetryDataForType(3, "MessageData");
    assertEquals("This is logback warn.", md4.getMessage());
    assertEquals(SeverityLevel.Warning, md4.getSeverityLevel());
    assertEquals("LOGBack", md4.getProperties().get("SourceType"));
    assertEquals("WARN", md4.getProperties().get("LoggingLevel"));

    MessageData md5 = getTelemetryDataForType(4, "MessageData");
    assertEquals("This is logback error.", md5.getMessage());
    assertEquals(SeverityLevel.Error, md5.getSeverityLevel());
    assertEquals("LOGBack", md5.getProperties().get("SourceType"));
    assertEquals("ERROR", md5.getProperties().get("LoggingLevel"));
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
    assertEquals("LOGBack", ed1.getProperties().get("SourceType"));
    assertEquals("ERROR", ed1.getProperties().get("LoggingLevel"));
  }
}
