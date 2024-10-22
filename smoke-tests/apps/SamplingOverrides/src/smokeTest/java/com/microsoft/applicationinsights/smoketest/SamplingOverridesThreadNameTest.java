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
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-thread-name.json")
abstract class SamplingOverridesThreadNameTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "/thread-name", callCount = 100)
  void testSampling() throws Exception {
    assertThat(testing.mockedIngestion.getCountForType("RequestData")).isZero();
    assertThat(testing.mockedIngestion.getCountForType("MessageData")).isZero();
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends SamplingOverridesThreadNameTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends SamplingOverridesThreadNameTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends SamplingOverridesThreadNameTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends SamplingOverridesThreadNameTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends SamplingOverridesThreadNameTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends SamplingOverridesThreadNameTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends SamplingOverridesThreadNameTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends SamplingOverridesThreadNameTest {}

  @Environment(TOMCAT_8_JAVA_23)
  static class Tomcat8Java23Test extends SamplingOverridesThreadNameTest {}

  @Environment(TOMCAT_8_JAVA_23_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends SamplingOverridesThreadNameTest {}

  // intentionally not including wildfly, since this test is set up for Tomcat thread names
}
