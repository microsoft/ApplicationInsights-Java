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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class ConsumptionPlanDisabledTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder()
          .doNotSetConnectionString()
          .setSkipHealthCheck(true)
          .setEnvVar("FUNCTIONS_WORKER_RUNTIME", "java")
          .setEnvVar("APPLICATIONINSIGHTS_ENABLE_AGENT", "false")
          // need to --add-opens for Java 17+ in order to set env var at runtime in SpringBootApp
          .addJvmArg("-XX:+IgnoreUnrecognizedVMOptions")
          .addJvmArg("--add-opens=java.base/java.util=ALL-UNNAMED")
          .build();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws InterruptedException {
    SECONDS.sleep(10);
    assertThat(testing.mockedIngestion.getItemCount()).isZero();
  }

  @Environment(JAVA_8)
  static class Java8Test extends ConsumptionPlanDisabledTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends ConsumptionPlanDisabledTest {}

  @Environment(JAVA_11)
  static class Java11Test extends ConsumptionPlanDisabledTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends ConsumptionPlanDisabledTest {}

  @Environment(JAVA_17)
  static class Java17Test extends ConsumptionPlanDisabledTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends ConsumptionPlanDisabledTest {}

  @Environment(JAVA_21)
  static class Java21Test extends ConsumptionPlanDisabledTest {}

  @Environment(JAVA_21_OPENJ9)
  static class Java21OpenJ9Test extends ConsumptionPlanDisabledTest {}
}
