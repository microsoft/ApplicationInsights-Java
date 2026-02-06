// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-disabled.json")
abstract class JavaProfilerDisabledTest {
  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.builder().build();

  @Test
  @TargetUri("/")
  void checkJavaProfilerDisabled() throws Exception {
    JavaProfilerEnabledTest.runTest(false, testing);
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends JavaProfilerDisabledTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends JavaProfilerDisabledTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends JavaProfilerDisabledTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends JavaProfilerDisabledTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends JavaProfilerDisabledTest {}
}
