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

import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
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
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request
                            .hasName("GET /Logback/test")
                            .hasNoSampleRate()
                            .hasTag("ai.operation.name", "GET /Logback/test")
                            .hasNoParent())
                .hasMessageCount(3)
                .hasMessageSatisying(
                    message ->
                        message
                            .hasMessage("This is logback warn.")
                            .hasSeverityLevel(SeverityLevel.WARNING)
                            .hasProperty("SourceType", "Logger")
                            .hasProperty("LoggerName", "smoketestapp")
                            .containsProperty("ThreadName")
                            .hasProperty("MDC key", "MDC value")
                            .hasParent(trace.getRequestId(0))
                            .hasNoSampleRate()
                            .hasTag("ai.operation.name", "GET /Logback/test")
                            .hasPropertiesSize(isWildflyServer() ? 4 : 8))
                .hasMessageSatisying(
                    message ->
                        message
                            .hasMessage("This is logback error.")
                            .hasSeverityLevel(SeverityLevel.ERROR)
                            .hasProperty("SourceType", "Logger")
                            .hasProperty("LoggerName", "smoketestapp")
                            .containsProperty("ThreadName")
                            .hasParent(trace.getRequestId(0))
                            .hasNoSampleRate()
                            .hasTag("ai.operation.name", "GET /Logback/test")
                            .hasPropertiesSize(isWildflyServer() ? 3 : 7)));

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
                            .hasNoSampleRate()
                            .hasTag("ai.operation.name", "GET /Logback/testWithException")
                            .hasNoParent())
                .hasExceptionCount(1)
                .hasExceptionSatisying(
                    exception ->
                        exception
                            .hasExceptionTypeName("java.lang.Exception")
                            .hasExceptionMessage("Fake Exception")
                            .hasSeverityLevel(SeverityLevel.ERROR)
                            .hasProperty("Logger Message", "This is an exception!")
                            .hasProperty("SourceType", "Logger")
                            .hasProperty("LoggerName", "smoketestapp")
                            .containsProperty("ThreadName")
                            .hasProperty("MDC key", "MDC value")
                            .hasParent(trace.getRequestId(0))
                            .hasNoSampleRate()
                            .hasTag("ai.operation.name", "GET /Logback/testWithException")
                            .hasPropertiesSize(isWildflyServer() ? 5 : 9)));

    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    if (!isWildflyServer()) {
      testing.waitAndAssertTrace(
          trace ->
              trace.hasExceptionSatisying(
                  exception ->
                      exception
                          .hasProperty("FileName", "LogbackWithExceptionServlet.java")
                          .hasProperty(
                              "ClassName",
                              "com.microsoft.applicationinsights.smoketestapp.LogbackWithExceptionServlet")
                          .hasProperty("MethodName", "doGet")
                          .hasProperty("LineNumber", "21")));
    }
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
