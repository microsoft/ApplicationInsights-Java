// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class LogbackTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  // Not really sure that Logback is enabled with Wildfly
  // https://anotheria.net/blog/devops/enable-logback-in-jboss/
  // https://www.oreilly.com/library/view/wildfly-cookbook/9781784392413/ch04s08.html
  boolean isWildflyServer() {
    return false;
  }

  @Test
  @TargetUri("/test")
  void test() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> mdList = testing.mockedIngestion.waitForMessageItemsInRequest(3, operationId);

    Envelope mdEnvelope1 = mdList.get(0);
    Envelope mdEnvelope2 = mdList.get(1);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(mdEnvelope1.getSampleRate()).isNull();
    assertThat(mdEnvelope2.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    List<MessageData> logs = testing.mockedIngestion.getMessageDataInRequest(3);
    logs.sort(Comparator.comparing(MessageData::getSeverityLevel));

    MessageData md1 = logs.get(0);
    MessageData md2 = logs.get(1);
    MessageData md3 = logs.get(2);

    assertThat(md1.getMessage()).isEqualTo("This is logback warn.");
    assertThat(md1.getSeverityLevel()).isEqualTo(SeverityLevel.WARNING);
    assertThat(md1.getProperties())
        .containsEntry("SourceType", "Logger")
        .containsEntry("LoggerName", "smoketestapp")
        .containsKey("ThreadName")
        .containsEntry("MDC key", "MDC value");

    if (!isWildflyServer()) {
      assertThat(md1.getProperties())
          .containsEntry("FileName", "LogbackServlet.java")
          .containsEntry(
              "ClassName", "com.microsoft.applicationinsights.smoketestapp.LogbackServlet")
          .containsEntry("MethodName", "doGet")
          .containsEntry("LineNumber", "26")
          .hasSize(8);
    } else {
      assertThat(md1.getProperties()).hasSize(4);
    }

    assertThat(md2.getMessage()).isEqualTo("This is logback error.");
    assertThat(md2.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
    assertThat(md2.getProperties())
        .containsEntry("SourceType", "Logger")
        .containsEntry("LoggerName", "smoketestapp")
        .containsKey("ThreadName");

    if (!isWildflyServer()) {
      assertThat(md2.getProperties())
          .containsEntry("FileName", "LogbackServlet.java")
          .containsEntry(
              "ClassName", "com.microsoft.applicationinsights.smoketestapp.LogbackServlet")
          .containsEntry("MethodName", "doGet")
          .containsEntry("LineNumber", "28")
          .hasSize(7);
    } else {
      assertThat(md2.getProperties()).hasSize(3);
    }

    if (!isWildflyServer()) {
      assertThat(md3.getProperties()).containsEntry("Marker", "aMarker");
    }

    SmokeTestExtension.assertParentChild(rd, rdEnvelope, mdEnvelope1, "GET /Logback/test");
    SmokeTestExtension.assertParentChild(rd, rdEnvelope, mdEnvelope2, "GET /Logback/test");
  }

  @Test
  @TargetUri("/testWithException")
  void testWithException() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope edEnvelope = edList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(edEnvelope.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    assertThat(ed.getExceptions().get(0).getMessage()).isEqualTo("Fake Exception");
    assertThat(ed.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
    assertThat(ed.getProperties())
        .containsEntry("Logger Message", "This is an exception!")
        .containsEntry("SourceType", "Logger")
        .containsEntry("LoggerName", "smoketestapp")
        .containsKey("ThreadName")
        .containsEntry("MDC key", "MDC value");

    if (!isWildflyServer()) {
      assertThat(ed.getProperties())
          .containsEntry("FileName", "LogbackWithExceptionServlet.java")
          .containsEntry(
              "ClassName",
              "com.microsoft.applicationinsights.smoketestapp.LogbackWithExceptionServlet")
          .containsEntry("MethodName", "doGet")
          .containsEntry("LineNumber", "21")
          .hasSize(9);
    } else {
      assertThat(ed.getProperties()).hasSize(5);
    }

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope, "GET /Logback/testWithException");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends LogbackTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends LogbackTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends LogbackTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends LogbackTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends LogbackTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends LogbackTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends LogbackTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends LogbackTest {
    @Override
    boolean isWildflyServer() {
      return true;
    }
  }

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends LogbackTest {
    @Override
    boolean isWildflyServer() {
      return true;
    }
  }
}
