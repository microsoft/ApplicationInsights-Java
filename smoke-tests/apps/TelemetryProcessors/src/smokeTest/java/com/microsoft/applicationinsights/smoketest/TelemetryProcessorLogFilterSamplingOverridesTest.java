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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-log-filtered-by-attribute.json")
abstract class TelemetryProcessorLogFilterSamplingOverridesTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/log-filtered-by-attribute")
  void logFilteredBySamplingOverrides() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);
    assertThat(telemetry.rd.getName())
        .isEqualTo("GET /TelemetryProcessors/log-filtered-by-attribute");

    assertThat(testing.mockedIngestion.getCountForType("MessageData")).isZero();
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test
      extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test
      extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test
      extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test
      extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java25Test extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java25OpenJ9Test
      extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends TelemetryProcessorLogFilterSamplingOverridesTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test
      extends TelemetryProcessorLogFilterSamplingOverridesTest {}
}
