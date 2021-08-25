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

@UseAgent
public class GrpcTest extends AiSmokeTest {

  @Test
  @TargetUri("/simple")
  public void doSimpleTest() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /simple");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/SayHello");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
    // individual messages are captured as OTel span events on the CLIENT/SERVER spans
    mockedIngestion.waitForItemsInOperation("MessageData", 2, operationId);

    Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "HelloController.simple");
    Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "example.Greeter/SayHello");

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd2 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();

    // TODO this is not correct (or at least not ideal)
    //  see https://msazure.visualstudio.com/One/_workitems/edit/8687985
    assertEquals("grpc", rdd2.getTarget());

    assertTrue(rd1.getProperties().isEmpty());
    assertTrue(rd1.getSuccess());

    assertTrue(rdd1.getProperties().isEmpty());
    assertTrue(rdd1.getSuccess());

    assertTrue(rdd2.getProperties().isEmpty());
    assertTrue(rdd2.getSuccess());

    // TODO (trask): verify rd2

    assertParentChild(rd1, rdEnvelope1, rddEnvelope1, "GET /simple");
    assertParentChild(rdd1, rddEnvelope1, rddEnvelope2, "GET /simple");
    assertParentChild(
        rdd2.getId(), rddEnvelope2, rdEnvelope2, "GET /simple", "example.Greeter/SayHello", false);
  }

  @Test
  @TargetUri("/conversation")
  public void doConversationTest() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /conversation");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/Conversation");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
    // individual messages are captured as OTel span events on the CLIENT/SERVER spans
    mockedIngestion.waitForItemsInOperation("MessageData", 3, operationId);

    Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "HelloController.conversation");
    Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "example.Greeter/Conversation");

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd2 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();

    assertEquals("grpc", rdd2.getTarget());

    assertTrue(rd1.getProperties().isEmpty());
    assertTrue(rd1.getSuccess());

    assertTrue(rdd1.getProperties().isEmpty());
    assertTrue(rdd1.getSuccess());

    assertTrue(rdd2.getProperties().isEmpty());
    assertTrue(rdd2.getSuccess());

    // TODO (trask): verify rd2

    assertParentChild(rd1, rdEnvelope1, rddEnvelope1, "GET /conversation");
    assertParentChild(rdd1, rddEnvelope1, rddEnvelope2, "GET /conversation");
    assertParentChild(
        rdd2.getId(),
        rddEnvelope2,
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
}
