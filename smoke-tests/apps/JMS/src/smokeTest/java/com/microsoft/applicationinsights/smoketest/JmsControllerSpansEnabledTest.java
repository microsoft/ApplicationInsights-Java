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

@Environment(JAVA_8)
@UseAgent("controller_spans_enabled_applicationinsights.json")
class JmsControllerSpansEnabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/sendMessage")
  void doMostBasicTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /sendMessage");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");
    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 3, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "message process");
    Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "HelloController.sendMessage");
    Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "message send");
    Envelope rddEnvelope3 = getDependencyEnvelope(rddList, "GET /");

    assertThat(rdEnvelope1.getSampleRate()).isNull();
    assertThat(rdEnvelope2.getSampleRate()).isNull();
    assertThat(rddEnvelope1.getSampleRate()).isNull();
    assertThat(rddEnvelope2.getSampleRate()).isNull();
    assertThat(rddEnvelope3.getSampleRate()).isNull();

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RequestData rd2 = (RequestData) ((Data<?>) rdEnvelope2.getData()).getBaseData();

    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd2 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();
    RemoteDependencyData rdd3 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope3.getData()).getBaseData();

    assertThat(rd1.getName()).isEqualTo("GET /sendMessage");
    assertThat(rd1.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd1.getName()).isEqualTo("HelloController.sendMessage");
    assertThat(rdd1.getData()).isNull();
    assertThat(rdd1.getType()).isEqualTo("InProc");
    assertThat(rdd1.getTarget()).isNull();
    assertThat(rdd1.getProperties()).isEmpty();
    assertThat(rdd1.getSuccess()).isTrue();

    assertThat(rdd2.getName()).isEqualTo("message send");
    assertThat(rdd2.getData()).isNull();
    assertThat(rdd2.getType()).isEqualTo("Queue Message | jms");
    assertThat(rdd2.getTarget()).isEqualTo("message");
    assertThat(rdd2.getProperties()).isEmpty();
    assertThat(rdd2.getSuccess()).isTrue();

    assertThat(rd2.getName()).isEqualTo("message process");
    assertThat(rd2.getSource()).isEqualTo("message");
    assertThat(rd2.getProperties()).isEmpty();
    assertThat(rd2.getSuccess()).isTrue();

    assertThat(rdd3.getName()).isEqualTo("GET /");
    assertThat(rdd3.getData()).isEqualTo("https://www.bing.com");
    assertThat(rdd3.getType()).isEqualTo("Http");
    assertThat(rdd3.getTarget()).isEqualTo("www.bing.com");
    assertThat(rdd3.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rdd3.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(rd1, rdEnvelope1, rddEnvelope1, "GET /sendMessage");
    SmokeTestExtension.assertParentChild(rdd1, rddEnvelope1, rddEnvelope2, "GET /sendMessage");
    SmokeTestExtension.assertParentChild(
        rdd2.getId(), rddEnvelope2, rdEnvelope2, "GET /sendMessage", "message process", false);
    SmokeTestExtension.assertParentChild(
        rd2.getId(), rdEnvelope2, rddEnvelope3, "message process", "message process", false);
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
}
