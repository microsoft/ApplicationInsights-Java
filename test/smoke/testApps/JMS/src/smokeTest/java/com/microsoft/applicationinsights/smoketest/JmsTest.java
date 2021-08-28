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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.Test;

@UseAgent
public class JmsTest extends AiSmokeTest {

  @Test
  @TargetUri("/sendMessage")
  public void doMostBasicTest() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /sendMessage");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");
    List<Envelope> rddList =
        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 3, operationId);
    assertEquals(0, mockedIngestion.getCountForType("EventData"));

    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "message process");
    Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "HelloController.sendMessage");
    Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "message send");
    Envelope rddEnvelope3 = getDependencyEnvelope(rddList, "HTTP GET");

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RequestData rd2 = (RequestData) ((Data<?>) rdEnvelope2.getData()).getBaseData();

    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd2 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();
    RemoteDependencyData rdd3 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope3.getData()).getBaseData();

    assertEquals("GET /sendMessage", rd1.getName());
    assertTrue(rd1.getProperties().isEmpty());
    assertTrue(rd1.getSuccess());

    assertEquals("HelloController.sendMessage", rdd1.getName());
    assertNull(rdd1.getData());
    assertEquals("InProc", rdd1.getType());
    assertNull(rdd1.getTarget());
    assertTrue(rdd1.getProperties().isEmpty());
    assertTrue(rdd1.getSuccess());

    assertEquals("message send", rdd2.getName());
    assertNull(rdd2.getData());
    assertEquals("Queue Message | jms", rdd2.getType());
    assertEquals("message", rdd2.getTarget());
    assertTrue(rdd2.getProperties().isEmpty());
    assertTrue(rdd2.getSuccess());

    assertEquals("message process", rd2.getName());
    assertEquals("message", rd2.getSource());
    assertTrue(rd2.getProperties().isEmpty());
    assertTrue(rd2.getSuccess());

    assertEquals("HTTP GET", rdd3.getName());
    assertEquals("https://www.bing.com", rdd3.getData());
    assertEquals("Http", rdd3.getType());
    assertEquals("www.bing.com", rdd3.getTarget());
    assertTrue(rdd3.getProperties().isEmpty());
    assertTrue(rdd3.getSuccess());

    assertParentChild(rd1, rdEnvelope1, rddEnvelope1, "GET /sendMessage");
    assertParentChild(rdd1, rddEnvelope1, rddEnvelope2, "GET /sendMessage");
    assertParentChild(
        rdd2.getId(), rddEnvelope2, rdEnvelope2, "GET /sendMessage", "message process", false);
    assertParentChild(
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
