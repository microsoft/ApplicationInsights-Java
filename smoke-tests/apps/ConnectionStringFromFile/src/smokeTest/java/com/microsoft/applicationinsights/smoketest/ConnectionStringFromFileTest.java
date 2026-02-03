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
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

// Verifies that ${file:...} connection string lookup works correctly when
// the APPLICATIONINSIGHTS_CONNECTION_STRING env var is NOT set.
@UseAgent
abstract class ConnectionStringFromFileTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder()
          .doNotSetConnectionString()
          .addFile("connection_string.txt", "/app/secrets/connection_string.txt")
          .build();

  @Test
  @TargetUri("/test")
  void shouldReadConnectionStringFromFile() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /ConnectionStringFromFile/*");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends ConnectionStringFromFileTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends ConnectionStringFromFileTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends ConnectionStringFromFileTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends ConnectionStringFromFileTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends ConnectionStringFromFileTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends ConnectionStringFromFileTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends ConnectionStringFromFileTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends ConnectionStringFromFileTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends ConnectionStringFromFileTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends ConnectionStringFromFileTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends ConnectionStringFromFileTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends ConnectionStringFromFileTest {}
}
