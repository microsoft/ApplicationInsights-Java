// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(TOMCAT_8_JAVA_8)
@UseAgent("level_off_applicationinsights.json")
class LogbackLevelOffTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void testDisabled() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request -> request.hasName("GET /Logback/test").hasSuccess(true))
                .hasMessageCount(0));
  }

  @Test
  @TargetUri("/testWithSpanException")
  void testWithSpanException() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request.hasName("GET /Logback/testWithSpanException").hasSuccess(true))
                .hasMessageCount(0)
                .hasExceptionCount(1)
                .hasExceptionSatisying(
                    exception ->
                        exception
                            .hasExceptionType("java.lang.RuntimeException")
                            .hasExceptionMessage("Test Exception")
                            .hasEmptyProperties())); // this is not a logger-based exception
  }
}
