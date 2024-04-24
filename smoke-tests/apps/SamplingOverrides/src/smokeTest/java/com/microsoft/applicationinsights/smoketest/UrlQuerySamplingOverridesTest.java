// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-urlquery.json")
abstract class UrlQuerySamplingOverridesTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().setUseDefaultHttpPort().build();

  @Test
  @TargetUri(value = "/test?query")
  void testUrlQuery() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /SamplingOverrides/*");
    assertThat(telemetry.rd.getUrl()).endsWith("http://localhost/SamplingOverrides/test?query");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends UrlQuerySamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends UrlQuerySamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends UrlQuerySamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends UrlQuerySamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends UrlQuerySamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends UrlQuerySamplingOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends UrlQuerySamplingOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends UrlQuerySamplingOverridesTest {}
}
