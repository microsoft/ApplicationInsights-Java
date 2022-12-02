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
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@UseAgent
abstract class SpringCloudStreamTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder()
          .setDependencyContainer(
              "KAFKA", new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1")))
          .build();

  @Test
  @TargetUri("/sendMessage")
  void doMostBasicTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = rdList.get(0);
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");
    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope rdEnvelope2 = rdList.get(1);
    Envelope rddEnvelope1 = rddList.get(0);

    assertThat(rdEnvelope1.getSampleRate()).isNull();
    assertThat(rdEnvelope2.getSampleRate()).isNull();
    assertThat(rddEnvelope1.getSampleRate()).isNull();

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RequestData rd2 = (RequestData) ((Data<?>) rdEnvelope2.getData()).getBaseData();
    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();

    assertThat(rd1.getName()).isEqualTo("GET /sendMessage");
    assertThat(rd1.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd1.getName()).isEqualTo("greetings send");
    assertThat(rdd1.getData()).isNull();
    assertThat(rdd1.getType()).isEqualTo("Queue Message | kafka");
    assertThat(rdd1.getTarget()).isEqualTo("greetings");
    assertThat(rdd1.getProperties()).isEmpty();
    assertThat(rdd1.getSuccess()).isTrue();

    assertThat(rd2.getName()).isEqualTo("greetings process");
    assertThat(rd2.getSource()).isEqualTo("greetings");
    assertThat(rd2.getProperties()).isEmpty();
    assertThat(rd2.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(rd1, rdEnvelope1, rddEnvelope1, "GET /sendMessage");
    SmokeTestExtension.assertParentChild(
        rdd1.getId(), rddEnvelope1, rdEnvelope2, "GET /sendMessage", "greetings process", false);
  }

  @Environment(JAVA_8)
  static class Java8Test extends SpringCloudStreamTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends SpringCloudStreamTest {}

  @Environment(JAVA_11)
  static class Java11Test extends SpringCloudStreamTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends SpringCloudStreamTest {}

  @Environment(JAVA_17)
  static class Java17Test extends SpringCloudStreamTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends SpringCloudStreamTest {}

  @Environment(JAVA_19)
  static class Java18Test extends SpringCloudStreamTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends SpringCloudStreamTest {}

  @Environment(JAVA_20)
  static class Java19Test extends SpringCloudStreamTest {}
}
