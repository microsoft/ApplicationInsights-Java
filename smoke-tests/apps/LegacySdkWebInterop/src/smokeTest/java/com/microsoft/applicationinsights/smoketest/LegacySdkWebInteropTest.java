/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class LegacySdkWebInteropTest {

  @RegisterExtension static final AiSmokeTest testing = new AiSmokeTest();

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
    assertThat(telemetry.rd.getProperties()).hasSize(2);

    assertThat(telemetry.rd.getSuccess()).isFalse();
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends LegacySdkWebInteropTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends LegacySdkWebInteropTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends LegacySdkWebInteropTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends LegacySdkWebInteropTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends LegacySdkWebInteropTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends LegacySdkWebInteropTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends LegacySdkWebInteropTest {}
}
