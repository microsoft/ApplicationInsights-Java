// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(TOMCAT_8_JAVA_8)
@UseAgent("disabled_applicationinsights.json")
class LogbackDisabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void testDisabled() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(request -> request.hasName("GET /Logback/test"))
                .hasMessageCount(0));
  }

  @Test
  @TargetUri("/testWithSpanException")
  void testWithSpanException() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request -> request.hasName("GET /Logback/testWithSpanException"))
                .hasMessageCount(0)
                // check that span exception is still captured
                .hasExceptionCount(1)
                .hasExceptionSatisfying(
                    exception ->
                        exception
                            .hasExceptionType("java.lang.RuntimeException")
                            .hasExceptionMessage("Test Exception")
                            .hasEmptyProperties())); // this is not a logger-based exception
  }
}
