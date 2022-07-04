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
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

@UseAgent
abstract class TraceLogBackTest {

  @Test
  @TargetUri("/traceLogBack")
  void testTraceLogBack() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> mdList = testing.mockedIngestion.waitForMessageItemsInRequest(2, operationId);

    Envelope mdEnvelope1 = mdList.get(0);
    Envelope mdEnvelope2 = mdList.get(1);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    List<MessageData> logs = testing.mockedIngestion.getMessageDataInRequest();
    logs.sort(Comparator.comparing(MessageData::getSeverityLevel));

    MessageData md1 = logs.get(0);
    MessageData md2 = logs.get(1);

    assertThat(md1.getMessage()).isEqualTo("This is logback warn.");
    assertEquals(SeverityLevel.WARNING, md1.getSeverityLevel());
    assertThat(md1.getProperties().get("SourceType")).isEqualTo("Logger");
    assertThat(md1.getProperties().get("LoggerName")).isEqualTo("smoketestapp");
    assertThat(md1.getProperties().get("ThreadName")).isNotNull();
    // TODO add MDC instrumentation for jboss logging
    if (!currentImageName.contains("wildfly")) {
      assertThat(md1.getProperties().get("MDC key")).isEqualTo("MDC value");
      assertThat(md1.getProperties()).hasSize(4);
    } else {
      assertThat(md1.getProperties()).hasSize(3);
    }

    assertThat(md2.getMessage()).isEqualTo("This is logback error.");
    assertEquals(SeverityLevel.ERROR, md2.getSeverityLevel());
    assertThat(md2.getProperties().get("SourceType")).isEqualTo("Logger");
    assertThat(md2.getProperties().get("LoggerName")).isEqualTo("smoketestapp");
    assertThat(md2.getProperties().get("ThreadName")).isNotNull();
    assertThat(md2.getProperties()).hasSize(3);

    AiSmokeTest.assertParentChild(rd, rdEnvelope, mdEnvelope1, "GET /TraceLogBack/traceLogBack");
    AiSmokeTest.assertParentChild(rd, rdEnvelope, mdEnvelope2, "GET /TraceLogBack/traceLogBack");
  }

  @Test
  @TargetUri("traceLogBackWithException")
  void testTraceLogBackWithExeption() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope edEnvelope = edList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    assertThat(ed.getExceptions().get(0).getMessage()).isEqualTo("Fake Exception");
    assertEquals(SeverityLevel.ERROR, ed.getSeverityLevel());
    assertThat(ed.getProperties().get("Logger Message")).isEqualTo("This is an exception!");
    assertThat(ed.getProperties().get("SourceType")).isEqualTo("Logger");
    assertThat(ed.getProperties().get("LoggerName")).isEqualTo("smoketestapp");
    assertThat(ed.getProperties().get("ThreadName")).isNotNull();
    // TODO add MDC instrumentation for jboss logging
    if (!currentImageName.contains("wildfly")) {
      assertThat(ed.getProperties().get("MDC key")).isEqualTo("MDC value");
      assertThat(ed.getProperties()).hasSize(5);
    } else {
      assertThat(ed.getProperties()).hasSize(4);
    }

    AiSmokeTest.assertParentChild(
        rd, rdEnvelope, edEnvelope, "GET /TraceLogBack/traceLogBackWithException");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends TraceLogBackTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends TraceLogBackTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends TraceLogBackTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends TraceLogBackTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends TraceLogBackTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends TraceLogBackTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends TraceLogBackTest {}
}
