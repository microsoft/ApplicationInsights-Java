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
  void test() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request
                            .hasName("GET /Logback/test")
                            .hasSuccess(true)
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
                            .hasPropertyKey("ThreadName")
                            .hasProperty("MDC key", "MDC value")
                            .satisfies(
                                md -> {
                                  if (!isWildflyServer()) {
                                    assertThat(md.getProperties())
                                        .containsEntry("FileName", "LogbackServlet.java")
                                        .containsEntry(
                                            "ClassName",
                                            "com.microsoft.applicationinsights.smoketestapp.LogbackServlet")
                                        .containsEntry("MethodName", "doGet")
                                        .containsEntry("LineNumber", "26")
                                        .hasSize(8);
                                  } else {
                                    assertThat(md.getProperties()).hasSize(4);
                                  }
                                }))
                .hasMessageSatisying(
                    message ->
                        message
                            .hasMessage("This is logback error.")
                            .hasSeverityLevel(SeverityLevel.ERROR)
                            .hasProperty("SourceType", "Logger")
                            .hasProperty("LoggerName", "smoketestapp")
                            .hasPropertyKey("ThreadName")
                            .satisfies(
                                md -> {
                                  if (!isWildflyServer()) {
                                    assertThat(md.getProperties())
                                        .containsEntry("FileName", "LogbackServlet.java")
                                        .containsEntry(
                                            "ClassName",
                                            "com.microsoft.applicationinsights.smoketestapp.LogbackServlet")
                                        .containsEntry("MethodName", "doGet")
                                        .containsEntry("LineNumber", "28")
                                        .hasSize(7);
                                  } else {
                                    assertThat(md.getProperties()).hasSize(3);
                                  }
                                }))
                .hasMessageSatisying(
                    message ->
                        message.satisfies(
                            md -> {
                              if (!isWildflyServer()) {
                                assertThat(md.getProperties()).containsEntry("Marker", "aMarker");
                              }
                            })));
  }

  @Test
  @TargetUri("/testWithException")
  void testWithException() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request
                            .hasName("GET /Logback/testWithException")
                            .hasSuccess(true)
                            .hasTag("ai.operation.name", "GET /Logback/testWithException")
                            .hasNoParent())
                .hasExceptionCount(1)
                .hasExceptionSatisying(
                    exception ->
                        exception
                            .hasExceptionType("java.lang.Exception")
                            .hasExceptionMessage("Fake Exception")
                            .hasSeverityLevel(SeverityLevel.ERROR)
                            .hasProperty("Logger Message", "This is an exception!")
                            .hasProperty("SourceType", "Logger")
                            .hasProperty("LoggerName", "smoketestapp")
                            .hasPropertyKey("ThreadName")
                            .hasProperty("MDC key", "MDC value")
                            .hasParent(trace.getRequestId(0))
                            .satisfies(
                                ed -> {
                                  if (!isWildflyServer()) {
                                    assertThat(ed.getProperties())
                                        .containsEntry(
                                            "FileName", "LogbackWithExceptionServlet.java")
                                        .containsEntry(
                                            "ClassName",
                                            "com.microsoft.applicationinsights.smoketestapp.LogbackWithExceptionServlet")
                                        .containsEntry("MethodName", "doGet")
                                        .containsEntry("LineNumber", "21")
                                        .hasSize(9);
                                  } else {
                                    assertThat(ed.getProperties()).hasSize(5);
                                  }
                                })));

    // Also ensure no EventData is sent
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();
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
