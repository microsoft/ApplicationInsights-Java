// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_18;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class CustomDimensionsTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getProperties()).containsEntry("test", "value");
    assertThat(telemetry.rd.getProperties()).containsKey("home");
    assertThat(telemetry.rd.getProperties()).hasSize(3);
    assertThat(telemetry.rd.getProperties())
        .containsEntry("_MS.ProcessedByMetricExtractors", "True");
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdEnvelope.getTags()).containsEntry("ai.application.ver", "123");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends CustomDimensionsTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends CustomDimensionsTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends CustomDimensionsTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends CustomDimensionsTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends CustomDimensionsTest {}

  @Environment(TOMCAT_8_JAVA_18)
  static class Tomcat8Java18Test extends CustomDimensionsTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends CustomDimensionsTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends CustomDimensionsTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends CustomDimensionsTest {}
}
