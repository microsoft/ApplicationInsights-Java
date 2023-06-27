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

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

@UseAgent
abstract class OpenTelemetryApiLogBridgeTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test-custom-exception-type-and-message")
  void testCustomExceptionTypeAndMessage() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("myspanname");
    assertThat(rd.getUrl())
        .matches("http://localhost:[0-9]+/OpenTelemetryApiLogBridge/test-custom-exception-type-and-message");
    assertThat(rd.getResponseCode()).isEqualTo("200");
    assertThat(rd.getSuccess()).isTrue();
    assertThat(rd.getSource()).isNull();
    assertThat(rd.getProperties()).hasSize(1);
    assertThat(rd.getProperties()).containsEntry("_MS.ProcessedByMetricExtractors", "True");

    assertThat(rdEnvelope.getIKey()).isEqualTo("00000000-0000-0000-0000-0FEEDDADBEEF");
    assertThat(rdEnvelope.getTags())
        .hasEntrySatisfying("ai.internal.sdkVersion", v -> assertThat(v).startsWith("java:3."));

    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);
    assertThat(edList.size()).isNotZero();
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends OpenTelemetryApiLogBridgeTest {}
}
