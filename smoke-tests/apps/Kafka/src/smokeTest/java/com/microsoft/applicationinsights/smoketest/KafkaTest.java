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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@UseAgent
abstract class KafkaTest {

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
    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    SECONDS.sleep(10);
    List<Envelope> mdList = testing.mockedIngestion.getItemsEnvelopeDataType("MetricData");
    for (Envelope mdItem : mdList) {
      MetricData md = (MetricData) ((Data<?>) mdItem.getData()).getBaseData();
      String name = md.getMetrics().get(0).getName();
      assertThat(name).doesNotContain("kafka");
    }

    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "mytopic process");
    Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "mytopic send");
    Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "GET /");

    assertThat(rdEnvelope1.getSampleRate()).isNull();
    assertThat(rdEnvelope2.getSampleRate()).isNull();
    assertThat(rddEnvelope1.getSampleRate()).isNull();
    assertThat(rddEnvelope2.getSampleRate()).isNull();

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

    assertThat(rdd1.getName()).isEqualTo("mytopic send");
    assertThat(rdd1.getData()).isNull();
    assertThat(rdd1.getType()).isEqualTo("Queue Message | kafka");
    assertThat(rdd1.getTarget()).isEqualTo("mytopic");
    assertThat(rdd1.getProperties()).isEmpty();
    assertThat(rdd1.getSuccess()).isTrue();

    assertThat(rd2.getName()).isEqualTo("mytopic process");
    assertThat(rd2.getSource()).isEqualTo("mytopic");
    assertThat(rd2.getProperties()).isEmpty();
    assertThat(rd2.getSuccess()).isTrue();

    assertThat(rdd2.getName()).isEqualTo("GET /");
    assertThat(rdd2.getData()).isEqualTo("https://www.bing.com");
    assertThat(rdd2.getType()).isEqualTo("Http");
    assertThat(rdd2.getTarget()).isEqualTo("www.bing.com");
    assertThat(rdd2.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rdd2.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(rd1, rdEnvelope1, rddEnvelope1, "GET /sendMessage");
    SmokeTestExtension.assertParentChild(
        rdd1.getId(), rddEnvelope1, rdEnvelope2, "GET /sendMessage", "mytopic process", false);
    SmokeTestExtension.assertParentChild(
        rd2.getId(), rdEnvelope2, rddEnvelope2, "mytopic process", "mytopic process", false);
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
  static class Java8Test extends KafkaTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends KafkaTest {}

  @Environment(JAVA_11)
  static class Java11Test extends KafkaTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends KafkaTest {}

  @Environment(JAVA_17)
  static class Java17Test extends KafkaTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends KafkaTest {}

  @Environment(JAVA_19)
  static class Java18Test extends KafkaTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends KafkaTest {}

  @Environment(JAVA_20)
  static class Java19Test extends KafkaTest {}
}
