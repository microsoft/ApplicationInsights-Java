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
abstract class OpenTelemetryApiSupportTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test-api")
  void testApi() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("myspanname");
    assertThat(telemetry.rd.getUrl())
        .matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/test-api");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties()).hasSize(3);
    assertThat(telemetry.rd.getProperties()).containsEntry("myattr1", "myvalue1");
    assertThat(telemetry.rd.getProperties()).containsEntry("myattr2", "myvalue2");
    assertThat(telemetry.rd.getProperties())
        .containsEntry("_MS.ProcessedByMetricExtractors", "True");

    assertThat(telemetry.rdEnvelope.getIKey()).isEqualTo("00000000-0000-0000-0000-0FEEDDADBEEF");
    assertThat(telemetry.rdEnvelope.getTags()).containsEntry("ai.cloud.role", "testrolename");
    assertThat(telemetry.rdEnvelope.getTags().get("ai.cloud.roleInstance"))
        .isEqualTo("testroleinstance");

    assertThat(telemetry.rdEnvelope.getTags())
        .hasEntrySatisfying("ai.internal.sdkVersion", v -> assertThat(v).startsWith("java:3."));
    assertThat(telemetry.rdEnvelope.getTags()).containsEntry("ai.user.id", "myuser");
  }

  @Test
  @TargetUri("/test-overriding-connection-string-etc")
  void testOverridingConnectionStringEtc() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName())
        .isEqualTo("GET /OpenTelemetryApiSupport/test-overriding-connection-string-etc");
    assertThat(telemetry.rd.getUrl())
        .matches(
            "http://localhost:[0-9]+/OpenTelemetryApiSupport/test-overriding-connection-string-etc");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    // checking that instrumentation key, cloud role name, cloud role instance, and sdk version are
    // from the agent

    // these are no longer supported since 3.4.0, but the test is still included here to (manually)
    // inspect that appropriate warning log about it no longer being supported is emitted
    //
    // assertThat(telemetry.rdEnvelope.getIKey()).isEqualTo("12341234-1234-1234-1234-123412341234");
    // assertThat(telemetry.rdEnvelope.getTags()).containsEntry("ai.cloud.role", "role-name-here");
    // assertThat(telemetry.rdEnvelope.getTags().get("ai.cloud.roleInstance"))
    //     .isEqualTo("role-instance-here");

    assertThat(telemetry.rdEnvelope.getTags())
        .containsEntry("ai.application.ver", "application-version-here");
    assertThat(telemetry.rdEnvelope.getTags())
        .hasEntrySatisfying("ai.internal.sdkVersion", v -> assertThat(v).startsWith("java:3."));
  }

  @Test
  @TargetUri("/test-extension-annotations")
  void testExtensionAnnotations() throws Exception {
    testAnnotations("test-extension-annotations", "underExtensionAnnotation");
  }

  @Test
  @TargetUri("/test-instrumentation-annotations")
  void testInstrumentationAnnotations() throws Exception {
    testAnnotations("test-instrumentation-annotations", "underInstrumentationAnnotation");
  }

  private static void testAnnotations(String path, String methodName) throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /OpenTelemetryApiSupport/" + path);
    assertThat(telemetry.rd.getUrl())
        .matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/" + path);
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    assertThat(telemetry.rdd1.getName()).isEqualTo("TestController." + methodName);
    assertThat(telemetry.rdd1.getData()).isNull();
    assertThat(telemetry.rdd1.getType()).isEqualTo("InProc");
    assertThat(telemetry.rdd1.getTarget()).isNull();
    assertThat(telemetry.rdd1.getProperties()).containsEntry("message", "a message");
    assertThat(telemetry.rdd1.getProperties()).hasSize(1);
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /OpenTelemetryApiSupport/" + path);
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends OpenTelemetryApiSupportTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends OpenTelemetryApiSupportTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends OpenTelemetryApiSupportTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends OpenTelemetryApiSupportTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends OpenTelemetryApiSupportTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends OpenTelemetryApiSupportTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends OpenTelemetryApiSupportTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends OpenTelemetryApiSupportTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends OpenTelemetryApiSupportTest {}
}
