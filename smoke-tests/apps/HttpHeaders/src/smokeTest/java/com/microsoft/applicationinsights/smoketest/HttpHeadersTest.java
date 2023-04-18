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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class HttpHeadersTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/serverHeaders")
  void testServerHeaders() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getProperties().get("http.response.header.abc"))
        .isEqualTo("testing123");
    assertThat(telemetry.rd.getProperties()).containsKey("http.request.header.host");
    assertThat(telemetry.rd.getProperties()).hasSize(3);
    assertThat(telemetry.rd.getProperties())
        .containsEntry("_MS.ProcessedByMetricExtractors", "True");
    assertThat(telemetry.rd.getSuccess()).isTrue();
  }

  @Test
  @TargetUri("/clientHeaders")
  void testClientHeaders() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getProperties()).containsKey("http.request.header.host");
    assertThat(telemetry.rd.getProperties()).hasSize(2);
    assertThat(telemetry.rd.getProperties())
        .containsEntry("_MS.ProcessedByMetricExtractors", "True");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rdd1.getProperties().get("http.request.header.abc"))
        .isEqualTo("testing123");
    assertThat(telemetry.rdd1.getProperties()).containsKey("http.response.header.date");
    assertThat(telemetry.rdd1.getProperties()).hasSize(3);
    assertThat(telemetry.rdd1.getProperties())
        .containsEntry("_MS.ProcessedByMetricExtractors", "True");
    assertThat(telemetry.rdd1.getSuccess()).isTrue();
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends HttpHeadersTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends HttpHeadersTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends HttpHeadersTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends HttpHeadersTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends HttpHeadersTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends HttpHeadersTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends HttpHeadersTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends HttpHeadersTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends HttpHeadersTest {}
}
