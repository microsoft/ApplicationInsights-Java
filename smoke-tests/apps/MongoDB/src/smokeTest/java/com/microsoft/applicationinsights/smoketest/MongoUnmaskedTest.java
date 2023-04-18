// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@WithDependencyContainers(
    @DependencyContainer(
        value = "mongo:4",
        exposedPort = 27017,
        hostnameEnvironmentVariable = "MONGO"))
@Environment(TOMCAT_8_JAVA_8)
@UseAgent("unmasked_applicationinsights.json")
class MongoUnmaskedTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/mongo")
  void mongo() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /MongoDB/*");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/MongoDB/mongo");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    assertThat(telemetry.rdd1.getName()).isEqualTo("find testdb.test");
    assertThat(telemetry.rdd1.getData()).isEqualTo("{\"find\": \"test\", \"$db\": \"testdb\"}");
    assertThat(telemetry.rdd1.getType()).isEqualTo("mongodb");
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+ \\| testdb");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /MongoDB/*");
  }
}
