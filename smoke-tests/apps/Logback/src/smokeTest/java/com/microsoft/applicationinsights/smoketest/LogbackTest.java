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
    testing.waitAndAssertTrace(
        trace -> {
          // Check request
          trace.hasRequestSatisying(
              request ->
                  request.hasNoSampleRate().hasTag("ai.operation.name", "GET /Logback/test"));

          // Check message count
          trace.hasMessageCount(3);

          // Sort messages by severity level to match original test logic
          List<Envelope> messages = trace.getMessages();
          messages.sort(
              Comparator.comparing(
                  envelope ->
                      ((MessageData) ((Data<?>) envelope.getData()).getBaseData())
                          .getSeverityLevel()));

          Envelope mdEnvelope1 = messages.get(0);
          Envelope mdEnvelope2 = messages.get(1);
          Envelope mdEnvelope3 = messages.get(2);

          // Assert sample rates
          assertThat(mdEnvelope1.getSampleRate()).isNull();
          assertThat(mdEnvelope2.getSampleRate()).isNull();

          // Assert first message (WARNING)
          new MessageAssert(mdEnvelope1)
              .hasMessage("This is logback warn.")
              .hasSeverityLevel(SeverityLevel.WARNING)
              .hasProperty("SourceType", "Logger")
              .hasProperty("LoggerName", "smoketestapp")
              .hasProperty("ThreadName")
              .hasProperty("MDC key", "MDC value");

          if (!isWildflyServer()) {
            new MessageAssert(mdEnvelope1)
                .hasProperty("FileName", "LogbackServlet.java")
                .hasProperty(
                    "ClassName", "com.microsoft.applicationinsights.smoketestapp.LogbackServlet")
                .hasProperty("MethodName", "doGet")
                .hasProperty("LineNumber", "26")
                .hasPropertyCount(8);
          } else {
            new MessageAssert(mdEnvelope1).hasPropertyCount(4);
          }

          // Assert second message (ERROR)
          new MessageAssert(mdEnvelope2)
              .hasMessage("This is logback error.")
              .hasSeverityLevel(SeverityLevel.ERROR)
              .hasProperty("SourceType", "Logger")
              .hasProperty("LoggerName", "smoketestapp")
              .hasProperty("ThreadName");

          if (!isWildflyServer()) {
            new MessageAssert(mdEnvelope2)
                .hasProperty("FileName", "LogbackServlet.java")
                .hasProperty(
                    "ClassName", "com.microsoft.applicationinsights.smoketestapp.LogbackServlet")
                .hasProperty("MethodName", "doGet")
                .hasProperty("LineNumber", "28")
                .hasPropertyCount(7);
          } else {
            new MessageAssert(mdEnvelope2).hasPropertyCount(3);
          }

          // Assert third message properties
          if (!isWildflyServer()) {
            new MessageAssert(mdEnvelope3).hasProperty("Marker", "aMarker");
          }
        });

    // Assert parent-child relationships separately using the original method
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> mdList = testing.mockedIngestion.waitForMessageItemsInRequest(3, operationId);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    SmokeTestExtension.assertParentChild(rd, rdEnvelope, mdList.get(0), "GET /Logback/test");
    SmokeTestExtension.assertParentChild(rd, rdEnvelope, mdList.get(1), "GET /Logback/test");
  }

  @Test
  @TargetUri("/testWithException")
  void testWithException() throws Exception {
    testing.waitAndAssertTrace(
        trace -> {
          // Check request
          trace.hasRequestSatisying(
              request ->
                  request
                      .hasNoSampleRate()
                      .hasTag("ai.operation.name", "GET /Logback/testWithException"));

          // Check exception count
          assertThat(trace.getExceptions()).hasSize(1);
          assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

          // Assert exception
          Envelope edEnvelope = trace.getExceptions().get(0);
          assertThat(edEnvelope.getSampleRate()).isNull();

          new ExceptionAssert(edEnvelope)
              .hasExceptionType("java.lang.Exception")
              .hasExceptionMessage("Fake Exception")
              .hasSeverityLevel(SeverityLevel.ERROR)
              .hasProperty("Logger Message", "This is an exception!")
              .hasProperty("SourceType", "Logger")
              .hasProperty("LoggerName", "smoketestapp")
              .hasProperty("ThreadName")
              .hasProperty("MDC key", "MDC value");

          if (!isWildflyServer()) {
            new ExceptionAssert(edEnvelope)
                .hasProperty("FileName", "LogbackWithExceptionServlet.java")
                .hasProperty(
                    "ClassName",
                    "com.microsoft.applicationinsights.smoketestapp.LogbackWithExceptionServlet")
                .hasProperty("MethodName", "doGet")
                .hasProperty("LineNumber", "21")
                .hasPropertyCount(9);
          } else {
            new ExceptionAssert(edEnvelope).hasPropertyCount(5);
          }
        });

    // Assert parent-child relationship separately using the original method
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edList.get(0), "GET /Logback/testWithException");
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
