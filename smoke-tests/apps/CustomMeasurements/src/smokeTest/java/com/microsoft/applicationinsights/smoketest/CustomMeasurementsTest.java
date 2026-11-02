// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_25;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_25_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class CustomMeasurementsTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder()
          .setAgentExtensionFile(new File("TestExtension/build/libs/extension.jar"))
          .build();

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

  @Environment(JAVA_8)
  static class Java8Test extends CustomMeasurementsTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends CustomMeasurementsTest {}

  @Environment(JAVA_11)
  static class Java11Test extends CustomMeasurementsTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends CustomMeasurementsTest {}

  @Environment(JAVA_17)
  static class Java17Test extends CustomMeasurementsTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends CustomMeasurementsTest {}

  @Environment(JAVA_21)
  static class Java21Test extends CustomMeasurementsTest {}

  @Environment(JAVA_21_OPENJ9)
  static class Java21OpenJ9Test extends CustomMeasurementsTest {}

  @Environment(JAVA_25)
  static class Java25Test extends CustomMeasurementsTest {}

  @Environment(JAVA_25_OPENJ9)
  static class Java25OpenJ9Test extends CustomMeasurementsTest {}
}
