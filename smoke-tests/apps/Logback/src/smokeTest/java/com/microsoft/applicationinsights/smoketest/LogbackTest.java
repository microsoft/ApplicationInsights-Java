// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
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
  void test() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request
                            .hasName("GET /Logback/test")
                            .hasSuccess(true)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasNoParent())
                .hasMessageCount(3)
                .hasMessageSatisying(
                    message ->
                        message
                            .hasMessage("This is logback warn.")
                            .hasSeverityLevel(SeverityLevel.WARNING)
                            .hasProperty("SourceType", "Logger")
                            .hasProperty("LoggerName", "smoketestapp")
                            .hasPropertyKey("ThreadName")
                            .hasProperty("MDC key", "MDC value")
                            .hasPropertiesSize(isWildflyServer() ? 4 : 8))
                .hasMessageSatisying(
                    message ->
                        message
                            .hasMessage("This is logback error.")
                            .hasSeverityLevel(SeverityLevel.ERROR)
                            .hasProperty("SourceType", "Logger")
                            .hasProperty("LoggerName", "smoketestapp")
                            .hasPropertyKey("ThreadName")
                            .hasPropertiesSize(isWildflyServer() ? 3 : 7)));

    // Additional assertions for non-Wildfly servers
    if (!isWildflyServer()) {
      testing.waitAndAssertTrace(
          trace ->
              trace
                  .hasMessageSatisying(
                      message ->
                          message
                              .hasMessage("This is logback warn.")
                              .hasProperty("FileName", "LogbackServlet.java")
                              .hasProperty(
                                  "ClassName",
                                  "com.microsoft.applicationinsights.smoketestapp.LogbackServlet")
                              .hasProperty("MethodName", "doGet")
                              .hasProperty("LineNumber", "26"))
                  .hasMessageSatisying(
                      message ->
                          message
                              .hasMessage("This is logback error.")
                              .hasProperty("FileName", "LogbackServlet.java")
                              .hasProperty(
                                  "ClassName",
                                  "com.microsoft.applicationinsights.smoketestapp.LogbackServlet")
                              .hasProperty("MethodName", "doGet")
                              .hasProperty("LineNumber", "28"))
                  .hasMessageSatisying(message -> message.hasProperty("Marker", "aMarker")));
    }
  }

  @Test
  @TargetUri("/testWithException")
  void testWithException() throws Exception {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request
                            .hasName("GET /Logback/testWithException")
                            .hasSuccess(true)
                            .hasProperty("_MS.ProcessedByMetricExtractors", "True")
                            .hasNoParent())
                .hasMessageCount(0)); // No MessageData, only ExceptionData

    // Check for exception data separately as it's not part of the trace assertion framework yet
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

    assertThat(ed.getExceptions().get(0).getTypeName()).isEqualTo("java.lang.Exception");
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

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends LogbackTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends LogbackTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends LogbackTest {}

  @Environment(TOMCAT_8_JAVA_23)
  static class Tomcat8Java23Test extends LogbackTest {}

  @Environment(TOMCAT_8_JAVA_23_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends LogbackTest {}

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
