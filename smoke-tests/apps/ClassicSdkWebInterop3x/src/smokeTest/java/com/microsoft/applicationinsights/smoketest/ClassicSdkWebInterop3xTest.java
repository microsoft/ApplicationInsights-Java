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
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class ClassicSdkWebInterop3xTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("myspanname");
    assertThat(telemetry.rd.getSource()).isEqualTo("mysource");
    assertThat(telemetry.rdEnvelope.getTags()).containsEntry("ai.user.id", "myuser");
    assertThat(telemetry.rdEnvelope.getTags()).containsEntry("ai.session.id", "mysessionid");
    assertThat(telemetry.rdEnvelope.getTags()).containsEntry("ai.device.os", "mydeviceos");
    assertThat(telemetry.rdEnvelope.getTags().get("ai.device.osVersion"))
        .isEqualTo("mydeviceosversion");
    assertThat(telemetry.rd.getProperties()).containsEntry("myattr1", "myvalue1");
    assertThat(telemetry.rd.getProperties()).containsEntry("myattr2", "myvalue2");
    assertThat(telemetry.rd.getProperties()).hasSize(3);
    assertThat(telemetry.rd.getProperties())
        .containsEntry("_MS.ProcessedByMetricExtractors", "True");

    assertThat(telemetry.rd.getSuccess()).isFalse();
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends ClassicSdkWebInterop3xTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends ClassicSdkWebInterop3xTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends ClassicSdkWebInterop3xTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends ClassicSdkWebInterop3xTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends ClassicSdkWebInterop3xTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends ClassicSdkWebInterop3xTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends ClassicSdkWebInterop3xTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends ClassicSdkWebInterop3xTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends ClassicSdkWebInterop3xTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends ClassicSdkWebInterop3xTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends ClassicSdkWebInterop3xTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends ClassicSdkWebInterop3xTest {}
}
