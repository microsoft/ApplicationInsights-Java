// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Environment(JAVA_8)
@UseAgent("disabled_applicationinsights.json")
class KafkaStreamsDisabledTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder()
          .setDependencyContainer(
              "KAFKA", new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1")))
          .build();

  @Test
  @TargetUri("/sendMessage")
  void doMostBasicTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    assertThat(rd.getName()).isEqualTo("GET /sendMessage");
    assertThat(rd.getResponseCode()).isEqualTo("200");
    assertThat(rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd.getSuccess()).isTrue();

    // verify the topology processed the message, and that no span was current while it ran
    List<Envelope> mdList =
        testing.mockedIngestion.waitForItems(
            "MessageData", KafkaStreamsDisabledTest::isTopologyLog, 1);
    Envelope mdEnvelope = mdList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(mdEnvelope.getSampleRate()).isNull();

    assertThat(mdEnvelope.getTags()).doesNotContainKey("ai.operation.id");

    // sleep a bit and make sure no kafka "requests" or dependencies are reported
    Thread.sleep(5000);
    assertThat(testing.mockedIngestion.getCountForType("RequestData")).isEqualTo(1);
    assertThat(testing.mockedIngestion.getCountForType("RemoteDependencyData")).isZero();
  }

  private static boolean isTopologyLog(Envelope envelope) {
    MessageData md = (MessageData) ((Data<?>) envelope.getData()).getBaseData();
    return "processing: hello world!".equals(md.getMessage());
  }
}
