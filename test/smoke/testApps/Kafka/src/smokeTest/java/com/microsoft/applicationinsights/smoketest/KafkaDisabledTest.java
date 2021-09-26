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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.Test;

@UseAgent("disabled_kafka")
@WithDependencyContainers({
  @DependencyContainer(
      value = "confluentinc/cp-zookeeper",
      portMapping = "2181",
      environmentVariables = {"ZOOKEEPER_CLIENT_PORT=2181"},
      hostnameEnvironmentVariable = "ZOOKEEPER"),
  @DependencyContainer(
      value = "confluentinc/cp-kafka",
      portMapping = "9092",
      environmentVariables = {
        "KAFKA_ZOOKEEPER_CONNECT=${ZOOKEEPER}:2181",
        "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://${CONTAINERNAME}:9092",
        "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1"
      },
      hostnameEnvironmentVariable = "KAFKA")
})
public class KafkaDisabledTest extends AiSmokeTest {

  @Test
  @TargetUri("/sendMessage")
  public void doMostBasicTest() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    assertEquals(0, mockedIngestion.getCountForType("EventData"));

    assertEquals("GET /sendMessage", rd.getName());
    assertEquals("200", rd.getResponseCode());
    assertTrue(rd.getProperties().isEmpty());
    assertTrue(rd.getSuccess());

    // verify the downstream http dependency that is no longer part of the same trace
    List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);
    Envelope rddEnvelope = rddList.get(0);
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertEquals("GET /", rdd.getName());
    assertEquals("https://www.bing.com", rdd.getData());
    assertTrue(rdd.getProperties().isEmpty());
    assertTrue(rdd.getSuccess());

    // sleep a bit and make sure no kafka "requests" or dependencies are reported
    Thread.sleep(5000);
    assertEquals(1, mockedIngestion.getCountForType("RequestData"));
    assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
  }
}
