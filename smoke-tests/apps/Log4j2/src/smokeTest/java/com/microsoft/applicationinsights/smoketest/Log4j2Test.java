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
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class Log4j2Test {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void test() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> mdList = testing.mockedIngestion.waitForMessageItemsInRequest(4, operationId);

    Envelope mdEnvelope1 = mdList.get(0);
    Envelope mdEnvelope2 = mdList.get(1);
    Envelope mdEnvelope3 = mdList.get(2);
    Envelope mdEnvelope4 = mdList.get(3);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(mdEnvelope1.getSampleRate()).isNull();
    assertThat(mdEnvelope2.getSampleRate()).isNull();
    assertThat(mdEnvelope3.getSampleRate()).isNull();
    assertThat(mdEnvelope4.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    List<MessageData> logs = testing.mockedIngestion.getMessageDataInRequest(4);
    logs.sort(Comparator.comparing(MessageData::getSeverityLevel));

    MessageData md1 = logs.get(0);
    MessageData md2 = logs.get(1);
    MessageData md3 = logs.get(2);
    MessageData md4 = logs.get(3);

    assertThat(md1.getMessage()).isEqualTo("This is log4j2 warn.");
    assertThat(md1.getSeverityLevel()).isEqualTo(SeverityLevel.WARNING);
    assertThat(md1.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(md1.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(md1.getProperties()).containsKey("ThreadName");
    assertThat(md1.getProperties()).containsEntry("MDC key", "MDC value");
    assertThat(md1.getProperties()).hasSize(4);

    assertThat(md2.getProperties()).containsEntry("Marker", "aMarker");

    assertThat(md3.getMessage()).isEqualTo("This is log4j2 error.");
    assertThat(md3.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
    assertThat(md3.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(md3.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(md3.getProperties()).containsKey("ThreadName");
    assertThat(md3.getProperties()).containsEntry("Map Message key", "Map Message value");
    assertThat(md3.getProperties()).hasSize(4);

    assertThat(md4.getMessage()).isEqualTo("This is log4j2 fatal.");
    assertThat(md4.getSeverityLevel()).isEqualTo(SeverityLevel.CRITICAL);
    assertThat(md4.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(md4.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(md4.getProperties()).containsKey("ThreadName");
    assertThat(md4.getProperties()).hasSize(3);

    SmokeTestExtension.assertParentChild(rd, rdEnvelope, mdEnvelope1, "GET /Log4j2/test");
    SmokeTestExtension.assertParentChild(rd, rdEnvelope, mdEnvelope2, "GET /Log4j2/test");
    SmokeTestExtension.assertParentChild(rd, rdEnvelope, mdEnvelope3, "GET /Log4j2/test");
    SmokeTestExtension.assertParentChild(rd, rdEnvelope, mdEnvelope4, "GET /Log4j2/test");
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

    List<ExceptionDetails> details = ed.getExceptions();
    ExceptionDetails ex = details.get(0);

    assertThat(ex.getMessage()).isEqualTo("Fake Exception");
    assertThat(ed.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
    assertThat(ed.getProperties()).containsEntry("Logger Message", "This is an exception!");
    assertThat(ed.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(ed.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(ed.getProperties()).containsKey("ThreadName");
    assertThat(ed.getProperties()).containsEntry("MDC key", "MDC value");
    assertThat(ed.getProperties()).hasSize(5);

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope, "GET /Log4j2/testWithException");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends Log4j2Test {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends Log4j2Test {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends Log4j2Test {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends Log4j2Test {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends Log4j2Test {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends Log4j2Test {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends Log4j2Test {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends Log4j2Test {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends Log4j2Test {}
}
