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

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
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
import org.junit.jupiter.api.Test;

@UseAgent
abstract class TraceLog4j2Test {

  @Test
  @TargetUri("/traceLog4j2")
  void testTraceLog4j2() throws Exception {
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

    assertThat(md1.getMessage()).isEqualTo("This is log4j2 warn.");
    assertEquals(SeverityLevel.WARNING, md1.getSeverityLevel());
    assertThat(md1.getProperties().get("SourceType")).isEqualTo("Logger");
    assertThat(md1.getProperties().get("LoggerName")).isEqualTo("smoketestapp");
    assertThat(md1.getProperties().get("ThreadName")).isNotNull();
    assertThat(md1.getProperties().get("MDC key")).isEqualTo("MDC value");
    assertThat(md1.getProperties()).hasSize(4);

    assertThat(md2.getMessage()).isEqualTo("This is log4j2 error.");
    assertEquals(SeverityLevel.ERROR, md2.getSeverityLevel());
    assertThat(md2.getProperties().get("SourceType")).isEqualTo("Logger");
    assertThat(md2.getProperties().get("LoggerName")).isEqualTo("smoketestapp");
    assertThat(md2.getProperties().get("ThreadName")).isNotNull();
    assertThat(md2.getProperties()).hasSize(3);

    assertThat(md3.getMessage()).isEqualTo("This is log4j2 fatal.");
    assertEquals(SeverityLevel.CRITICAL, md3.getSeverityLevel());
    assertThat(md3.getProperties().get("SourceType")).isEqualTo("Logger");
    assertThat(md3.getProperties().get("LoggerName")).isEqualTo("smoketestapp");
    assertThat(md3.getProperties().get("ThreadName")).isNotNull();
    assertThat(md3.getProperties()).hasSize(3);

    AiSmokeTest.assertParentChild(rd, rdEnvelope, mdEnvelope1, "GET /TraceLog4j2/traceLog4j2");
    AiSmokeTest.assertParentChild(rd, rdEnvelope, mdEnvelope2, "GET /TraceLog4j2/traceLog4j2");
    AiSmokeTest.assertParentChild(rd, rdEnvelope, mdEnvelope3, "GET /TraceLog4j2/traceLog4j2");
  }

  @Test
  @TargetUri("/traceLog4j2WithException")
  void testTraceLog4j2WithException() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope edEnvelope = edList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    List<ExceptionDetails> details = ed.getExceptions();
    ExceptionDetails ex = details.get(0);

    assertThat(ex.getMessage()).isEqualTo("Fake Exception");
    assertEquals(SeverityLevel.ERROR, ed.getSeverityLevel());
    assertThat(ed.getProperties().get("Logger Message")).isEqualTo("This is an exception!");
    assertThat(ed.getProperties().get("SourceType")).isEqualTo("Logger");
    assertThat(ed.getProperties().get("LoggerName")).isEqualTo("smoketestapp");
    assertThat(ed.getProperties().get("ThreadName")).isNotNull();
    assertThat(ed.getProperties().get("MDC key")).isEqualTo("MDC value");
    assertThat(ed.getProperties()).hasSize(5);

    AiSmokeTest.assertParentChild(
        rd, rdEnvelope, edEnvelope, "GET /TraceLog4j2/traceLog4j2WithException");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends TraceLog4j2Test {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends TraceLog4j2Test {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends TraceLog4j2Test {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends TraceLog4j2Test {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends TraceLog4j2Test {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends TraceLog4j2Test {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends TraceLog4j2Test {}
}
