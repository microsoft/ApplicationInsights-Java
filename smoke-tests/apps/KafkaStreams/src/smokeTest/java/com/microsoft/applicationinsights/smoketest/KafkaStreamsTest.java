// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_25;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_25_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@UseAgent
abstract class KafkaStreamsTest {

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

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /sendMessage");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");
    // "output publish" is only part of the same operation when the kafka-streams instrumentation
    // makes "input process" the current span while the topology runs
    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
    List<Envelope> mdList =
        testing.mockedIngestion.waitForItemsInOperation(
            "MessageData", 1, operationId, KafkaStreamsTest::isTopologyLog);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "input process");
    Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "input publish");
    Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "output publish");
    Envelope mdEnvelope = mdList.get(0);

    assertThat(rdEnvelope1.getSampleRate()).isNull();
    assertThat(rdEnvelope2.getSampleRate()).isNull();
    assertThat(rddEnvelope1.getSampleRate()).isNull();
    assertThat(rddEnvelope2.getSampleRate()).isNull();
    assertThat(mdEnvelope.getSampleRate()).isNull();

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RequestData rd2 = (RequestData) ((Data<?>) rdEnvelope2.getData()).getBaseData();

    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd2 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();

    assertThat(rd1.getName()).isEqualTo("GET /sendMessage");
    assertThat(rd1.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd1.getName()).isEqualTo("input publish");
    assertThat(rdd1.getData()).isNull();
    assertThat(rdd1.getType()).isEqualTo("Queue Message | kafka");
    assertThat(rdd1.getTarget()).isEqualTo("input");
    assertThat(rdd1.getProperties()).isEmpty();
    assertThat(rdd1.getSuccess()).isTrue();

    assertThat(rd2.getName()).isEqualTo("input process");
    assertThat(rd2.getSource()).isEqualTo("input");
    assertThat(rd2.getProperties()).isEmpty();
    assertThat(rd2.getSuccess()).isTrue();

    assertThat(rdd2.getName()).isEqualTo("output publish");
    assertThat(rdd2.getData()).isNull();
    assertThat(rdd2.getType()).isEqualTo("Queue Message | kafka");
    assertThat(rdd2.getTarget()).isEqualTo("output");
    assertThat(rdd2.getProperties()).isEmpty();
    assertThat(rdd2.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(rd1, rdEnvelope1, rddEnvelope1, "GET /sendMessage");
    SmokeTestExtension.assertParentChild(
        rdd1.getId(), rddEnvelope1, rdEnvelope2, "GET /sendMessage", "input process", false);
    SmokeTestExtension.assertParentChild(
        rd2.getId(), rdEnvelope2, rddEnvelope2, "input process", "input process", false);
    assertThat(mdEnvelope.getTags()).containsEntry("ai.operation.parentId", rd2.getId());

    // sleep a bit and make sure no duplicate "process" requests or extra dependencies are reported
    SECONDS.sleep(5);
    assertThat(testing.mockedIngestion.getCountForType("RequestData")).isEqualTo(2);
    assertThat(testing.mockedIngestion.getCountForType("RemoteDependencyData")).isEqualTo(2);
  }

  private static boolean isTopologyLog(Envelope envelope) {
    MessageData md = (MessageData) ((Data<?>) envelope.getData()).getBaseData();
    return "processing: hello world!".equals(md.getMessage());
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

  private static Envelope getDependencyEnvelope(List<Envelope> envelopes, String name) {
    for (Envelope envelope : envelopes) {
      RemoteDependencyData rdd =
          (RemoteDependencyData) ((Data<?>) envelope.getData()).getBaseData();
      if (rdd.getName().equals(name)) {
        return envelope;
      }
    }
    throw new IllegalStateException("Could not find dependency with name: " + name);
  }

  @Environment(JAVA_8)
  static class Java8Test extends KafkaStreamsTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends KafkaStreamsTest {}

  @Environment(JAVA_11)
  static class Java11Test extends KafkaStreamsTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends KafkaStreamsTest {}

  @Environment(JAVA_17)
  static class Java17Test extends KafkaStreamsTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends KafkaStreamsTest {}

  @Environment(JAVA_21)
  static class Java21Test extends KafkaStreamsTest {}

  @Environment(JAVA_21_OPENJ9)
  static class Java21OpenJ9Test extends KafkaStreamsTest {}

  @Environment(JAVA_25)
  static class Java25Test extends KafkaStreamsTest {}

  @Environment(JAVA_25_OPENJ9)
  static class Java25OpenJ9Test extends KafkaStreamsTest {}
}
