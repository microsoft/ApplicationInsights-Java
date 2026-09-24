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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class CustomMeasurementsTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test-custom-measurements")
  void customMeasurementsOnSpanAndLogRecord() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);
    assertThat(telemetry.rd.getMeasurements())
        .containsEntry("itemsProcessed", 42.0)
        .containsEntry("queueDepth", 7.0);
    assertThat(telemetry.rd.getProperties()).doesNotContainKey("microsoft.custom_measurements");

    List<MessageData> logs = testing.mockedIngestion.getMessageDataInRequest(1);
    MessageData log = logs.get(0);
    assertThat(log.getMessage()).isEqualTo("custom measurements");
    assertThat(log.getMeasurements())
        .containsEntry("itemsProcessed", 42.0)
        .containsEntry("queueDepth", 7.0);
    assertThat(log.getProperties()).doesNotContainKey("microsoft.custom_measurements");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends CustomMeasurementsTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends CustomMeasurementsTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends CustomMeasurementsTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends CustomMeasurementsTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends CustomMeasurementsTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends CustomMeasurementsTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends CustomMeasurementsTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends CustomMeasurementsTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends CustomMeasurementsTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends CustomMeasurementsTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends CustomMeasurementsTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends CustomMeasurementsTest {}
}
