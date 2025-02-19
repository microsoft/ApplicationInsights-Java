// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class ConsumptionPlanEnabledTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder()
          .doNotSetConnectionString()
          .setSkipHealthCheck(true) // to be consistent with the disabled test
          .setEnvVar("FUNCTIONS_WORKER_RUNTIME", "java")
          .setEnvVar("APPLICATIONINSIGHTS_ENABLE_AGENT", "true")
          // need to --add-opens for Java 17+ in order to set env var at runtime in SpringBootApp
          .addJvmArg("-XX:+IgnoreUnrecognizedVMOptions")
          .addJvmArg("--add-opens=java.base/java.util=ALL-UNNAMED")
          .build();

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
  }

  @Environment(JAVA_8)
  static class Java8Test extends ConsumptionPlanEnabledTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends ConsumptionPlanEnabledTest {}

  @Environment(JAVA_11)
  static class Java11Test extends ConsumptionPlanEnabledTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends ConsumptionPlanEnabledTest {}

  @Environment(JAVA_17)
  static class Java17Test extends ConsumptionPlanEnabledTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends ConsumptionPlanEnabledTest {}

  @Environment(JAVA_21)
  static class Java21Test extends ConsumptionPlanEnabledTest {}

  @Environment(JAVA_21_OPENJ9)
  static class Java21OpenJ9Test extends ConsumptionPlanEnabledTest {}
}
