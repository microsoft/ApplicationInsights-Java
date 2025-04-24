// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.EventData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class OpenTelemetryApiLogBridgeTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test-custom-exception-type-and-message")
  void testCustomExceptionTypeAndMessage() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getUrl())
        .matches(
            "http://localhost:[0-9]+/OpenTelemetryApiLogBridge/test-custom-exception-type-and-message");
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
    ExceptionData ed = (ExceptionData) ((Data<?>) edList.get(0).getData()).getBaseData();
    assertThat(ed.getExceptions().get(0).getTypeName()).isEqualTo("my exception type");
    assertThat(ed.getExceptions().get(0).getMessage())
        .isEqualTo("This is an custom exception with custom exception type");
  }

  @Test
  @TargetUri("/test-custom-event")
  void testCustomEvent() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getUrl())
        .matches("http://localhost:[0-9]+/OpenTelemetryApiLogBridge/test-custom-event");
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
        testing.mockedIngestion.waitForItemsInOperation("EventData", 1, operationId);
    assertThat(edList.size()).isNotZero();
    EventData ed = (EventData) ((Data<?>) edList.get(0).getData()).getBaseData();
    assertThat(ed.getName()).isEqualTo("my_custom_event");
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

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(TOMCAT_8_JAVA_23)
  static class Tomcat8Java23Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(TOMCAT_8_JAVA_23_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends OpenTelemetryApiLogBridgeTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends OpenTelemetryApiLogBridgeTest {}
}
