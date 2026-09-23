// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25_OPENJ9;

import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class LogbackFluentLoggingTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void test() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request.hasName("GET /LogbackFluentLogging/test").hasSuccess(true).hasNoSampleRate())
                .hasMessageCount(2)
                .hasMessageSatisfying(
                    message ->
                        message
                            .hasMessage("This is logback warn.")
                            .hasSeverityLevel(SeverityLevel.WARNING)
                            .hasProperty("FileName", "LogbackFluentLoggingServlet.java")
                            .hasProperty("ClassName", "com.microsoft.applicationinsights.smoketestapp.LogbackFluentLoggingServlet")
                            .hasProperty("MethodName", "doGet")
                            .hasProperty("LineNumber", "27")
                            .hasProperty("SourceType", "Logger")
                            .hasProperty("LoggerName", "smoketestapp")
                            .hasPropertyKey("ThreadName")
                            .hasProperty("MDC key", "MDC value")
                            .hasProperty("Marker", "aMarker")
                            .hasProperty("customKey", "customValue")
                            .hasNoSampleRate()
                            .hasPropertiesSize(10)
                )
                .hasMessageSatisfying(
                    message ->
                        message
                            .hasMessage("This is logback error.")
                            .hasSeverityLevel(SeverityLevel.ERROR)
                            .hasProperty("FileName", "LogbackFluentLoggingServlet.java")
                            .hasProperty("ClassName", "com.microsoft.applicationinsights.smoketestapp.LogbackFluentLoggingServlet")
                            .hasProperty("MethodName", "doGet")
                            .hasProperty("LineNumber", "28")
                            .hasProperty("SourceType", "Logger")
                            .hasProperty("LoggerName", "smoketestapp")
                            .hasPropertyKey("ThreadName")
                            .hasProperty("Marker", "aMarker")
                            .hasProperty("customKey", "customValue")
                            .hasPropertiesSize(10)
                            .hasNoSampleRate()
                ));
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
                            .hasName("GET /LogbackFluentLogging/testWithException")
                            .hasSuccess(true)
                            .hasNoSampleRate())
                .hasExceptionCount(1)
                .hasExceptionSatisfying(
                    exception ->
                        exception
                            .hasExceptionType("java.lang.Exception")
                            .hasExceptionMessage("Fake Exception")
                            .hasSeverityLevel(SeverityLevel.ERROR)
                            .hasProperty("FileName", "LogbackFluentLoggingWithExceptionServlet.java")
                            .hasProperty("ClassName", "com.microsoft.applicationinsights.smoketestapp.LogbackFluentLoggingWithExceptionServlet")
                            .hasProperty("MethodName", "doGet")
                            .hasProperty("LineNumber", "24")
                            .hasPropertiesSize(11)
                            .hasProperty("Logger Message", "This is an exception!")
                            .hasProperty("SourceType", "Logger")
                            .hasProperty("LoggerName", "smoketestapp")
                            .hasPropertyKey("ThreadName")
                            .hasProperty("MDC key", "MDC value")
                            .hasProperty("Marker", "aMarker")
                            .hasProperty("customKey", "customValue")
                            .hasNoSampleRate()));
  }


  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends LogbackFluentLoggingTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends LogbackFluentLoggingTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends LogbackFluentLoggingTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends LogbackFluentLoggingTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends LogbackFluentLoggingTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends LogbackFluentLoggingTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends LogbackFluentLoggingTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends LogbackFluentLoggingTest {}

}
