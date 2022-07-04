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

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class GrpcTest {

  @RegisterExtension static final AiSmokeTest testing = new AiSmokeTest();

  @Test
  @TargetUri("/simple")
  public void doSimpleTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /simple");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/SayHello");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    // auto-collected grpc events are suppressed by exporter because they are noisy
    assertThat(testing.mockedIngestion.getCountForType("MessageData", operationId)).isZero();

    Envelope rddEnvelope = getDependencyEnvelope(rddList, "example.Greeter/SayHello");

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertThat(rdd.getTarget()).isEqualTo("localhost:10203");

    assertThat(rd1.getProperties()).isEmpty();
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd.getProperties()).isEmpty();
    assertThat(rdd.getSuccess()).isTrue();

    // TODO (trask): verify rd2

    AiSmokeTest.assertParentChild(rd1, rdEnvelope1, rddEnvelope, "GET /simple");
    AiSmokeTest.assertParentChild(
        rdd.getId(), rddEnvelope, rdEnvelope2, "GET /simple", "example.Greeter/SayHello", false);
  }

  @Test
  @TargetUri("/conversation")
  public void doConversationTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /conversation");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/Conversation");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    // auto-collected grpc events are suppressed by exporter because they are noisy
    assertThat(testing.mockedIngestion.getCountForType("MessageData", operationId)).isZero();

    Envelope rddEnvelope = getDependencyEnvelope(rddList, "example.Greeter/Conversation");

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertThat(rdd.getTarget()).isEqualTo("localhost:10203");

    assertThat(rd1.getProperties()).isEmpty();
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd.getProperties()).isEmpty();
    assertThat(rdd.getSuccess()).isTrue();

    // TODO (trask): verify rd2

    AiSmokeTest.assertParentChild(rd1, rdEnvelope1, rddEnvelope, "GET /conversation");
    AiSmokeTest.assertParentChild(
        rdd.getId(),
        rddEnvelope,
        rdEnvelope2,
        "GET /conversation",
        "example.Greeter/Conversation",
        false);
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
  static class Java8Test extends GrpcTest {}

  @Environment(JAVA_11)
  static class Java11Test extends GrpcTest {}

  @Environment(JAVA_17)
  static class Java17Test extends GrpcTest {}
}
