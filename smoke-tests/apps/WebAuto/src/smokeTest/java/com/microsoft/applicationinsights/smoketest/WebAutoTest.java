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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class WebAutoTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    testing.mockedIngestion.waitForItems("RequestData", 1);
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends WebAutoTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends WebAutoTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends WebAutoTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends WebAutoTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends WebAutoTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends WebAutoTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends WebAutoTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends WebAutoTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends WebAutoTest {}
}
