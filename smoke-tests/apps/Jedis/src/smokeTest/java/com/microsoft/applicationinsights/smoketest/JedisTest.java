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
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
@WithDependencyContainers(
    @DependencyContainer(
        value = "redis",
        exposedPort = 6379,
        hostnameEnvironmentVariable = "REDIS"))
abstract class JedisTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/jedis")
  void jedis() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /Jedis/*");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/Jedis/jedis");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    assertThat(telemetry.rdd1.getName()).isEqualTo("GET");
    assertThat(telemetry.rdd1.getData()).isEqualTo("GET test");
    assertThat(telemetry.rdd1.getType()).isEqualTo("redis");
    assertThat(telemetry.rdd1.getTarget()).matches("dependency[0-9]+");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /Jedis/*");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends JedisTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends JedisTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends JedisTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends JedisTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends JedisTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends JedisTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends JedisTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends JedisTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends JedisTest {}
}
