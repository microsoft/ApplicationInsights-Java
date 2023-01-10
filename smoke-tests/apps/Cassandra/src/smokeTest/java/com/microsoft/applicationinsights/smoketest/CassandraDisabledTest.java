// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(TOMCAT_8_JAVA_8)
@UseAgent("disabled_applicationinsights.json")
@WithDependencyContainers(
    @DependencyContainer(
        value = "cassandra:3",
        exposedPort = 9042,
        hostnameEnvironmentVariable = "CASSANDRA"))
class CassandraDisabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/cassandra")
  void cassandra() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /Cassandra/*");
    assertThat(telemetry.rd.getSuccess()).isTrue();

    // sleep a bit and make sure no cassandra dependencies are reported
    Thread.sleep(5000);
    assertThat(testing.mockedIngestion.getCountForType("RemoteDependencyData")).isZero();
  }
}
