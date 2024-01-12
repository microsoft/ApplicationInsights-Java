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

import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-http-semconv.json")
abstract class TelemetryProcessorsStableSemconv {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getProperties()).containsEntry("attribute1", "testValue1");
    assertThat(telemetry.rd.getProperties()).containsEntry("attribute2", "testValue2");
    assertThat(telemetry.rd.getProperties()).containsEntry("sensitiveAttribute1", "sensitiveData1");
    assertThat(telemetry.rd.getProperties()).containsEntry("httpPath", "/TelemetryProcessors/test");
    assertThat(telemetry.rd.getProperties()).hasSize(5);
    assertThat(telemetry.rd.getProperties())
        .containsEntry("_MS.ProcessedByMetricExtractors", "True");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    // Log processor test
    List<MessageData> logs = testing.mockedIngestion.getMessageDataInRequest(1);
    MessageData md1 = logs.get(0);
    assertThat(md1.getMessage()).isEqualTo("testValue1::testValue2");
  }

  @Test
  @TargetUri("/sensitivedata")
  void doSimpleTestPiiData() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("testValue1::testValue2");
    assertThat(telemetry.rd.getProperties()).containsEntry("attribute1", "testValue1");
    assertThat(telemetry.rd.getProperties()).containsEntry("attribute2", "testValue2");
    assertThat(telemetry.rd.getProperties()).containsEntry("sensitiveAttribute1", "redacted");
    assertThat(telemetry.rd.getProperties())
        .containsEntry("httpPath", "/TelemetryProcessors/sensitivedata");
    assertThat(telemetry.rd.getProperties()).hasSize(5);
    assertThat(telemetry.rd.getProperties())
        .containsEntry("_MS.ProcessedByMetricExtractors", "True");
    assertThat(telemetry.rd.getSuccess()).isTrue();
  }

  @Test
  @TargetUri("/user/123")
  void shouldMask() throws Exception {

    Telemetry telemetry = testing.getTelemetry(0);

    String url = telemetry.rd.getUrl();

    assertThat(url).startsWith("http://localhost:").endsWith("/TelemetryProcessors/user/***");
    // We don't test the port that is different for each test execution

  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends TelemetryProcessorsStableSemconv {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends TelemetryProcessorsStableSemconv {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends TelemetryProcessorsStableSemconv {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends TelemetryProcessorsStableSemconv {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends TelemetryProcessorsStableSemconv {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends TelemetryProcessorsStableSemconv {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends TelemetryProcessorsStableSemconv {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends TelemetryProcessorsStableSemconv {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends TelemetryProcessorsStableSemconv {}
}
