/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;

@UseAgent
public class TraceLog4j2Test extends AiWarSmokeTest {

  @Test
  @TargetUri("/traceLog4j2")
  public void testTraceLog4j2() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> mdList = testing.mockedIngestion.waitForMessageItemsInRequest(3, operationId);

    Envelope mdEnvelope1 = mdList.get(0);
    Envelope mdEnvelope2 = mdList.get(1);
    Envelope mdEnvelope3 = mdList.get(2);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    List<MessageData> logs = testing.mockedIngestion.getMessageDataInRequest();
    logs.sort(Comparator.comparing(MessageData::getSeverityLevel));

    MessageData md1 = logs.get(0);
    MessageData md2 = logs.get(1);
    MessageData md3 = logs.get(2);

    assertEquals("This is log4j2 warn.", md1.getMessage());
    assertEquals(SeverityLevel.WARNING, md1.getSeverityLevel());
    assertEquals("Logger", md1.getProperties().get("SourceType"));
    assertEquals("smoketestapp", md1.getProperties().get("LoggerName"));
    assertThat(md1.getProperties().get("ThreadName")).isNotNull();
    assertEquals("MDC value", md1.getProperties().get("MDC key"));
    assertThat(md1.getProperties()).hasSize(4);

    assertEquals("This is log4j2 error.", md2.getMessage());
    assertEquals(SeverityLevel.ERROR, md2.getSeverityLevel());
    assertEquals("Logger", md2.getProperties().get("SourceType"));
    assertEquals("smoketestapp", md1.getProperties().get("LoggerName"));
    assertThat(md1.getProperties().get("ThreadName")).isNotNull();
    assertThat(md2.getProperties()).hasSize(3);

    assertEquals("This is log4j2 fatal.", md3.getMessage());
    assertEquals(SeverityLevel.CRITICAL, md3.getSeverityLevel());
    assertEquals("Logger", md3.getProperties().get("SourceType"));
    assertEquals("smoketestapp", md3.getProperties().get("LoggerName"));
    assertThat(md3.getProperties().get("ThreadName")).isNotNull();
    assertThat(md3.getProperties()).hasSize(3);

    AiSmokeTest.assertParentChild(
        rd, rdEnvelope, mdEnvelope1, "GET /TraceLog4j2UsingAgent/traceLog4j2");
    AiSmokeTest.assertParentChild(
        rd, rdEnvelope, mdEnvelope2, "GET /TraceLog4j2UsingAgent/traceLog4j2");
    AiSmokeTest.assertParentChild(
        rd, rdEnvelope, mdEnvelope3, "GET /TraceLog4j2UsingAgent/traceLog4j2");
  }

  @Test
  @TargetUri("/traceLog4j2WithException")
  public void testTraceLog4j2WithException() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);
    assertEquals(0, testing.mockedIngestion.getCountForType("EventData"));

    Envelope edEnvelope = edList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    List<ExceptionDetails> details = ed.getExceptions();
    ExceptionDetails ex = details.get(0);

    assertEquals("Fake Exception", ex.getMessage());
    assertEquals(SeverityLevel.ERROR, ed.getSeverityLevel());
    assertEquals("This is an exception!", ed.getProperties().get("Logger Message"));
    assertEquals("Logger", ed.getProperties().get("SourceType"));
    assertEquals("smoketestapp", ed.getProperties().get("LoggerName"));
    assertThat(ed.getProperties().get("ThreadName")).isNotNull();
    assertEquals("MDC value", ed.getProperties().get("MDC key"));
    assertThat(ed.getProperties()).hasSize(5);

    AiSmokeTest.assertParentChild(
        rd, rdEnvelope, edEnvelope, "GET /TraceLog4j2UsingAgent/traceLog4j2WithException");
  }
}
