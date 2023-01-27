// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_18_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

abstract class RuntimeAttachTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /test");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/test");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    assertThat(telemetry.rdEnvelope.getTags())
        .hasEntrySatisfying("ai.internal.sdkVersion", v -> assertThat(v).startsWith("ra_java:3."));
  }

  @Environment(JAVA_8)
  static class Java8Test extends RuntimeAttachTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends RuntimeAttachTest {}

  @Environment(JAVA_11)
  static class Java11Test extends RuntimeAttachTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends RuntimeAttachTest {}

  @Environment(JAVA_17)
  static class Java17Test extends RuntimeAttachTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends RuntimeAttachTest {}

  @Environment(JAVA_19)
  static class Java18Test extends RuntimeAttachTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends RuntimeAttachTest {}

  @Environment(JAVA_20)
  static class Java19Test extends RuntimeAttachTest {}
}
