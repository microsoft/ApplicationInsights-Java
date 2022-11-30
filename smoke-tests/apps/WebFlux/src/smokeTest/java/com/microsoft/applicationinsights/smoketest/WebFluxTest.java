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

@UseAgent
abstract class WebFluxTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /test/**");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/test");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();
  }

  @Test
  @TargetUri("/exception")
  void testException() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /exception");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/exception");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("500");
    assertThat(telemetry.rd.getSuccess()).isFalse();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();
  }

  @Test
  @TargetUri("/futureException")
  void testFutureException() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /futureException");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/futureException");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("500");
    assertThat(telemetry.rd.getSuccess()).isFalse();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();
  }

  @Environment(JAVA_8)
  static class Java8Test extends WebFluxTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends WebFluxTest {}

  @Environment(JAVA_11)
  static class Java11Test extends WebFluxTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends WebFluxTest {}

  @Environment(JAVA_17)
  static class Java17Test extends WebFluxTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends WebFluxTest {}

  @Environment(JAVA_19)
  static class Java18Test extends WebFluxTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends WebFluxTest {}

  @Environment(JAVA_20)
  static class Java19Test extends WebFluxTest {}
}
