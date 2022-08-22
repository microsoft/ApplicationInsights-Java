/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(JAVA_8)
@UseAgent("disabled_applicationinsights.json")
@WithDependencyContainers({
  @DependencyContainer(
      value = "confluentinc/cp-zookeeper",
      exposedPort = 2181,
      environmentVariables = {"ZOOKEEPER_CLIENT_PORT=2181"},
      hostnameEnvironmentVariable = "ZOOKEEPER"),
  @DependencyContainer(
      value = "confluentinc/cp-kafka",
      exposedPort = 9092,
      environmentVariables = {
        "KAFKA_ZOOKEEPER_CONNECT=${ZOOKEEPER}:2181",
        "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://${CONTAINERNAME}:9092",
        "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1"
      },
      hostnameEnvironmentVariable = "KAFKA")
})
class KafkaDisabledTest {

  @RegisterExtension static final SmokeTestExtension testing = new SmokeTestExtension();

  @Test
  @TargetUri("/sendMessage")
  void doMostBasicTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    assertThat(rd.getName()).isEqualTo("GET /sendMessage");
    assertThat(rd.getResponseCode()).isEqualTo("200");
    assertThat(rd.getProperties().get("_MS.ProcessedByMetricExtractors")).isEqualTo("True");
    assertThat(rd.getSuccess()).isTrue();

    // verify the downstream http dependency that is no longer part of the same trace
    List<Envelope> rddList = testing.mockedIngestion.waitForItems("RemoteDependencyData", 1);
    Envelope rddEnvelope = rddList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(rddEnvelope.getSampleRate()).isNull();

    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertThat(rdd.getName()).isEqualTo("GET /");
    assertThat(rdd.getData()).isEqualTo("https://www.bing.com");
    assertThat(rdd.getProperties().get("_MS.ProcessedByMetricExtractors")).isEqualTo("True");
    assertThat(rdd.getSuccess()).isTrue();

    // sleep a bit and make sure no kafka "requests" or dependencies are reported
    Thread.sleep(5000);
    assertThat(testing.mockedIngestion.getCountForType("RequestData")).isEqualTo(1);
    assertThat(testing.mockedIngestion.getCountForType("RemoteDependencyData")).isEqualTo(1);
  }
}
