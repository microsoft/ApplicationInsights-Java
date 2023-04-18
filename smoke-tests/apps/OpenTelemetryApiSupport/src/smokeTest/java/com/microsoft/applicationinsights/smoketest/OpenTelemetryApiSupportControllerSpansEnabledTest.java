// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(TOMCAT_8_JAVA_8)
@UseAgent("controller_spans_enabled_applicationinsights.json")
class OpenTelemetryApiSupportControllerSpansEnabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test-api")
  void testApi() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /OpenTelemetryApiSupport/test-api");
    assertThat(telemetry.rd.getUrl())
        .matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/test-api");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    assertThat(telemetry.rdd1.getName()).isEqualTo("myspanname");
    assertThat(telemetry.rdd1.getData()).isNull();
    assertThat(telemetry.rdd1.getType()).isEqualTo("InProc");
    assertThat(telemetry.rdd1.getTarget()).isNull();
    assertThat(telemetry.rdd1.getProperties()).hasSize(2);
    assertThat(telemetry.rdd1.getProperties()).containsEntry("myattr1", "myvalue1");
    assertThat(telemetry.rdd1.getProperties()).containsEntry("myattr2", "myvalue2");
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    // ideally want the properties below on rd, but can't get SERVER span yet
    // see
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1726#issuecomment-731890267

    // checking that instrumentation key, cloud role name, cloud role instance, and sdk version are
    // from the agent
    assertThat(telemetry.rddEnvelope1.getIKey()).isEqualTo("00000000-0000-0000-0000-0FEEDDADBEEF");
    assertThat(telemetry.rddEnvelope1.getTags()).containsEntry("ai.cloud.role", "testrolename");
    assertThat(telemetry.rddEnvelope1.getTags().get("ai.cloud.roleInstance"))
        .isEqualTo("testroleinstance");
    assertThat(telemetry.rddEnvelope1.getTags())
        .hasEntrySatisfying("ai.internal.sdkVersion", v -> assertThat(v).startsWith("java:3."));
    assertThat(telemetry.rddEnvelope1.getTags()).containsEntry("ai.user.id", "myuser");

    SmokeTestExtension.assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /OpenTelemetryApiSupport/test-api");
  }

  @Test
  @TargetUri("/test-extension-annotations")
  void testExtensionAnnotations() throws Exception {
    testAnnotations(
        "test-extension-annotations", "testExtensionAnnotations", "underExtensionAnnotation");
  }

  @Test
  @TargetUri("/test-instrumentation-annotations")
  void testInstrumentationAnnotations() throws Exception {
    testAnnotations(
        "test-instrumentation-annotations",
        "testInstrumentationAnnotations",
        "underInstrumentationAnnotation");
  }

  private static void testAnnotations(
      String path, String controllerMethodName, String annotatedMethodName) throws Exception {
    Telemetry telemetry = testing.getTelemetry(2);

    if (!telemetry.rdd1.getName().equals("TestController." + controllerMethodName)) {
      RemoteDependencyData rddTemp = telemetry.rdd1;
      telemetry.rdd1 = telemetry.rdd2;
      telemetry.rdd2 = rddTemp;

      Envelope rddEnvelopeTemp = telemetry.rddEnvelope1;
      telemetry.rddEnvelope1 = telemetry.rddEnvelope2;
      telemetry.rddEnvelope2 = rddEnvelopeTemp;
    }

    assertThat(telemetry.rd.getName()).isEqualTo("GET /OpenTelemetryApiSupport/" + path);
    assertThat(telemetry.rd.getUrl())
        .matches("http://localhost:[0-9]+/OpenTelemetryApiSupport/" + path);
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    assertThat(telemetry.rdd1.getName()).isEqualTo("TestController." + controllerMethodName);
    assertThat(telemetry.rdd1.getData()).isNull();
    assertThat(telemetry.rdd1.getType()).isEqualTo("InProc");
    assertThat(telemetry.rdd1.getTarget()).isNull();
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    assertThat(telemetry.rdd2.getName()).isEqualTo("TestController." + annotatedMethodName);
    assertThat(telemetry.rdd2.getData()).isNull();
    assertThat(telemetry.rdd2.getType()).isEqualTo("InProc");
    assertThat(telemetry.rdd2.getTarget()).isNull();
    assertThat(telemetry.rdd2.getProperties()).containsEntry("message", "a message");
    assertThat(telemetry.rdd2.getProperties()).hasSize(1);
    assertThat(telemetry.rdd2.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /OpenTelemetryApiSupport/" + path);
    SmokeTestExtension.assertParentChild(
        telemetry.rdd1,
        telemetry.rddEnvelope1,
        telemetry.rddEnvelope2,
        "GET /OpenTelemetryApiSupport/" + path);
  }
}
