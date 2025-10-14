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

import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-delete-existing-attribute.json")
abstract class TelemetryProcessorDeleteLogAttribute {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/delete-existing-log-attribute")
  void deleteExistingLogAttribute() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);
    assertThat(telemetry.rd.getName())
        .isEqualTo("GET /TelemetryProcessors/delete-existing-log-attribute");
    assertThat(telemetry.rd.getProperties().get("toBeDeletedAttributeKey")).isNull();

    List<MessageData> logs = testing.mockedIngestion.getMessageDataInRequest(1);
    MessageData log = logs.get(0);
    assertThat(log.getMessage()).isEqualTo("custom property from MDC");
    Map<String, String> properties = log.getProperties();
    assertThat(properties.get("toBeDeletedAttributeKey")).isNull();
    assertThat(properties.get("LoggerName")).isEqualTo("smoketestappcontroller");
    assertThat(properties.get("SourceType")).isEqualTo("Logger");
    assertThat(properties.get("ThreadName")).isNotNull();
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

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends TelemetryProcessorDeleteLogAttribute {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends TelemetryProcessorDeleteLogAttribute {}
}
