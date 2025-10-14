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
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights2.json")
abstract class SamplingOverridesBackCompat2Test {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "/login", callCount = 100)
  void testSampling() throws Exception {
    testing.mockedIngestion.waitForItems("RequestData", 100);
    testing.mockedIngestion.waitForItems("RemoteDependencyData", 100);
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends SamplingOverridesBackCompat2Test {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends SamplingOverridesBackCompat2Test {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends SamplingOverridesBackCompat2Test {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends SamplingOverridesBackCompat2Test {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends SamplingOverridesBackCompat2Test {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends SamplingOverridesBackCompat2Test {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends SamplingOverridesBackCompat2Test {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends SamplingOverridesBackCompat2Test {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends SamplingOverridesBackCompat2Test {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends SamplingOverridesBackCompat2Test {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends SamplingOverridesBackCompat2Test {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends SamplingOverridesBackCompat2Test {}
}
