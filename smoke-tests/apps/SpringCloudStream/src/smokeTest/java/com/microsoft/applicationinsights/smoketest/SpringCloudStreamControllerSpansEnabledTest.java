// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
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

@Environment(JAVA_8)
@UseAgent("controller_spans_enabled_applicationinsights.json")
class SpringCloudStreamControllerSpansEnabledTest {

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
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope rdEnvelope2 = rdList.get(1);
    Envelope rddEnvelope1 = rddList.get(0);
    Envelope rddEnvelope2 = rddList.get(1);

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

    if (!rdd1.getName().equals("GreetingsController.sendMessage")) {
      RemoteDependencyData rddTemp = rdd1;
      rdd1 = rdd2;
      rdd2 = rddTemp;

      Envelope rddEnvelopeTemp = rddEnvelope1;
      rddEnvelope1 = rddEnvelope2;
      rddEnvelope2 = rddEnvelopeTemp;
    }

    assertThat(rd1.getName()).isEqualTo("GET /sendMessage");
    assertThat(rd1.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd1.getName()).isEqualTo("GreetingsController.sendMessage");
    assertThat(rdd1.getData()).isNull();
    assertThat(rdd1.getType()).isEqualTo("InProc");
    assertThat(rdd1.getTarget()).isNull();
    assertThat(rdd1.getProperties()).isEmpty();
    assertThat(rdd1.getSuccess()).isTrue();

    assertThat(rdd2.getName()).isEqualTo("greetings send");
    assertThat(rdd2.getData()).isNull();
    assertThat(rdd2.getType()).isEqualTo("Queue Message | kafka");
    assertThat(rdd2.getTarget()).isEqualTo("greetings");
    assertThat(rdd2.getProperties()).isEmpty();
    assertThat(rdd2.getSuccess()).isTrue();

    assertThat(rd2.getName()).isEqualTo("greetings process");
    assertThat(rd2.getSource()).isEqualTo("greetings");
    assertThat(rd2.getProperties()).isEmpty();
    assertThat(rd2.getSuccess()).isTrue();
    assertThat(rd2.getMeasurements()).containsKey("timeSinceEnqueued");

    SmokeTestExtension.assertParentChild(rd1, rdEnvelope1, rddEnvelope1, "GET /sendMessage");
    SmokeTestExtension.assertParentChild(rdd1, rddEnvelope1, rddEnvelope2, "GET /sendMessage");
    SmokeTestExtension.assertParentChild(
        rdd2.getId(), rddEnvelope2, rdEnvelope2, "GET /sendMessage", "greetings process", false);
  }
}
