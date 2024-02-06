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
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-delete-existing-attribute.json")
abstract class TelemetryProcessorDeleteLogAttribute {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().setSelfDiagnosticsLevel("DEBUG").build();

  @Test
  @TargetUri("/delete-existing-log-attribute")
  void deleteExistingLogAttribute() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);
    assertThat(telemetry.rd.getName())
        .isEqualTo("GET /TelemetryProcessors/delete-existing-log-attribute");
    assertThat(telemetry.rd.getProperties().get("toBeDeletedAttributeKey")).isNull();

    List<MessageData> logs = testing.mockedIngestion.getTelemetryDataByType("MessageData");
    logs.stream()
        .filter(log -> log.getMessage().contains("custom property from MDC"))
        .forEach(
            log -> {
              assertThat(log.getProperties().get("toBeDeletedAttributeKey")).isNull();
            });
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends TelemetryProcessorDeleteLogAttribute {}
}
