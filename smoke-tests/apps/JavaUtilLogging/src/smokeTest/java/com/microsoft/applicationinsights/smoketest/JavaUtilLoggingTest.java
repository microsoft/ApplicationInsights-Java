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
abstract class JavaUtilLoggingTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void test() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> mdList = testing.mockedIngestion.waitForMessageItemsInRequest(2, operationId);

    Envelope mdEnvelope1 = mdList.get(0);
    Envelope mdEnvelope2 = mdList.get(1);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(mdEnvelope1.getSampleRate()).isNull();
    assertThat(mdEnvelope2.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    List<MessageData> logs = testing.mockedIngestion.getMessageDataInRequest(2);
    logs.sort(Comparator.comparing(MessageData::getSeverityLevel));

    MessageData md1 = logs.get(0);
    MessageData md2 = logs.get(1);

    assertThat(md1.getMessage()).isEqualTo("This is jul warning.");
    assertThat(md1.getSeverityLevel()).isEqualTo(SeverityLevel.WARNING);
    assertThat(md1.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(md1.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(md1.getProperties()).containsKey("ThreadName");
    assertThat(md1.getProperties()).hasSize(3);

    assertThat(md2.getMessage()).isEqualTo("This is jul severe.");
    assertThat(md2.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
    assertThat(md2.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(md2.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(md2.getProperties()).containsKey("ThreadName");
    assertThat(md2.getProperties()).hasSize(3);

    SmokeTestExtension.assertParentChild(rd, rdEnvelope, mdEnvelope1, "GET /JavaUtilLogging/test");
    SmokeTestExtension.assertParentChild(rd, rdEnvelope, mdEnvelope2, "GET /JavaUtilLogging/test");
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
    assertThat(ed.getProperties()).containsEntry("Logger Message", "This is an exception!");
    assertThat(ed.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(ed.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(ed.getProperties()).containsKey("ThreadName");
    assertThat(ed.getProperties()).hasSize(4);

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope, "GET /JavaUtilLogging/testWithException");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends JavaUtilLoggingTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends JavaUtilLoggingTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends JavaUtilLoggingTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends JavaUtilLoggingTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends JavaUtilLoggingTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends JavaUtilLoggingTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends JavaUtilLoggingTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends JavaUtilLoggingTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends JavaUtilLoggingTest {}
}
