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
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_18;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_18_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_19;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class JmsTest {

  @RegisterExtension static final SmokeTestExtension testing = new SmokeTestExtension();

  @Test
  @TargetUri("/sendMessage")
  void doMostBasicTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /sendMessage");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");
    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "message process");
    Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "message send");
    Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "GET /");

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RequestData rd2 = (RequestData) ((Data<?>) rdEnvelope2.getData()).getBaseData();

    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd2 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();

    assertThat(rd1.getName()).isEqualTo("GET /sendMessage");
    assertThat(rd1.getProperties()).isEmpty();
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd1.getName()).isEqualTo("message send");
    assertThat(rdd1.getData()).isNull();
    assertThat(rdd1.getType()).isEqualTo("Queue Message | jms");
    assertThat(rdd1.getTarget()).isEqualTo("message");
    assertThat(rdd1.getProperties()).isEmpty();
    assertThat(rdd1.getSuccess()).isTrue();

    assertThat(rd2.getName()).isEqualTo("message process");
    assertThat(rd2.getSource()).isEqualTo("message");
    assertThat(rd2.getProperties()).isEmpty();
    assertThat(rd2.getSuccess()).isTrue();

    assertThat(rdd2.getName()).isEqualTo("GET /");
    assertThat(rdd2.getData()).isEqualTo("https://www.bing.com");
    assertThat(rdd2.getType()).isEqualTo("Http");
    assertThat(rdd2.getTarget()).isEqualTo("www.bing.com");
    assertThat(rdd2.getProperties()).isEmpty();
    assertThat(rdd2.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(rd1, rdEnvelope1, rddEnvelope1, "GET /sendMessage");
    SmokeTestExtension.assertParentChild(
        rdd1.getId(), rddEnvelope1, rdEnvelope2, "GET /sendMessage", "message process", false);
    SmokeTestExtension.assertParentChild(
        rd2.getId(), rdEnvelope2, rddEnvelope2, "message process", "message process", false);
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
  static class Java8Test extends JmsTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends JmsTest {}

  @Environment(JAVA_11)
  static class Java11Test extends JmsTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends JmsTest {}

  @Environment(JAVA_17)
  static class Java17Test extends JmsTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends JmsTest {}

  @Environment(JAVA_18)
  static class Java18Test extends JmsTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends JmsTest {}

  @Environment(JAVA_19)
  static class Java19Test extends JmsTest {}
}
