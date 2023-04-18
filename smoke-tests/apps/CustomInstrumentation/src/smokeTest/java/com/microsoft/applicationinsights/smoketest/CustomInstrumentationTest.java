// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_18_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
public abstract class CustomInstrumentationTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/internal-span")
  void internalSpan() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    String operationId = telemetry.rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> mdList = testing.mockedIngestion.waitForMessageItemsInRequest(1, operationId);

    Envelope mdEnvelope = mdList.get(0);

    assertThat(telemetry.rdEnvelope.getSampleRate()).isNull();
    assertThat(telemetry.rddEnvelope1.getSampleRate()).isNull();
    assertThat(mdEnvelope.getSampleRate()).isNull();

    MessageData md = (MessageData) ((Data<?>) mdEnvelope.getData()).getBaseData();

    assertThat(telemetry.rd.getName()).isEqualTo("GET /internal-span");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getName()).isEqualTo("TestController.run");
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    assertThat(md.getMessage()).isEqualTo("hello");
    assertThat(md.getSeverityLevel()).isEqualTo(SeverityLevel.INFORMATION);
    assertThat(md.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(md.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(md.getProperties()).containsKey("ThreadName");
    assertThat(md.getProperties()).hasSize(3);

    SmokeTestExtension.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /internal-span");

    SmokeTestExtension.assertParentChild(
        telemetry.rdd1, telemetry.rddEnvelope1, mdEnvelope, "GET /internal-span");
  }

  @Test
  @TargetUri("/server-span")
  void serverSpan() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /server-span");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "TestController.run");

    String operationId = rdEnvelope2.getTags().get("ai.operation.id");
    List<Envelope> mdList = testing.mockedIngestion.waitForMessageItemsInRequest(1, operationId);

    Envelope mdEnvelope = mdList.get(0);

    assertThat(rdEnvelope1.getSampleRate()).isNull();
    assertThat(rdEnvelope2.getSampleRate()).isNull();
    assertThat(mdEnvelope.getSampleRate()).isNull();

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RequestData rd2 = (RequestData) ((Data<?>) rdEnvelope2.getData()).getBaseData();
    MessageData md = (MessageData) ((Data<?>) mdEnvelope.getData()).getBaseData();

    assertThat(rd1.getName()).isEqualTo("GET /server-span");
    assertThat(rd1.getResponseCode()).isEqualTo("200");
    assertThat(rd1.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rd2.getName()).isEqualTo("TestController.run");
    assertThat(rd2.getResponseCode()).isEqualTo("0");
    assertThat(rd2.getProperties()).isEmpty();
    assertThat(rd2.getSuccess()).isTrue();

    assertThat(md.getMessage()).isEqualTo("hello");
    assertThat(md.getSeverityLevel()).isEqualTo(SeverityLevel.INFORMATION);
    assertThat(md.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(md.getProperties()).containsEntry("LoggerName", "smoketestapp");
    assertThat(md.getProperties()).containsKey("ThreadName");
    assertThat(md.getProperties()).hasSize(3);

    SmokeTestExtension.assertParentChild(rd2, rdEnvelope2, mdEnvelope, "TestController.run");
  }

  private static Envelope getRequestEnvelope(List<Envelope> envelopes, String name) {
    for (Envelope envelope : envelopes) {
      RequestData rd = (RequestData) ((Data<?>) envelope.getData()).getBaseData();
      if (rd.getName().equals(name)) {
        return envelope;
      }
    }
    throw new IllegalStateException("Could not find request with name: " + name);
  }

  @Environment(JAVA_8)
  static class Java8Test extends CustomInstrumentationTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends CustomInstrumentationTest {}

  @Environment(JAVA_11)
  static class Java11Test extends CustomInstrumentationTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends CustomInstrumentationTest {}

  @Environment(JAVA_17)
  static class Java17Test extends CustomInstrumentationTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends CustomInstrumentationTest {}

  @Environment(JAVA_19)
  static class Java18Test extends CustomInstrumentationTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends CustomInstrumentationTest {}

  @Environment(JAVA_20)
  static class Java19Test extends CustomInstrumentationTest {}
}
