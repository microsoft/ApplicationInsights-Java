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
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class GrpcTest {

  @RegisterExtension static final SmokeTestExtension testing = new SmokeTestExtension();

  @Test
  @TargetUri("/simple")
  void doSimpleTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 2);
    List<Envelope> rpcClientDurationMetrics =
        testing.mockedIngestion.waitForItems(
            SmokeTestExtension.getMetricPredicate("rpc.client.duration"), 2, 40, TimeUnit.SECONDS);
    List<Envelope> rpcServerMetrics =
        testing.mockedIngestion.waitForItems(
            SmokeTestExtension.getMetricPredicate("rpc.server.duration"), 2, 40, TimeUnit.SECONDS);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /simple");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/SayHello");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    // auto-collected grpc events are suppressed by exporter because they are noisy
    assertThat(testing.mockedIngestion.getCountForType("MessageData", operationId)).isZero();

    Envelope rddEnvelope = getDependencyEnvelope(rddList, "example.Greeter/SayHello");

    assertThat(rdEnvelope1.getSampleRate()).isNull();
    assertThat(rdEnvelope2.getSampleRate()).isNull();
    assertThat(rddEnvelope.getSampleRate()).isNull();

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertThat(rdd.getTarget()).isEqualTo("localhost:10203");

    assertThat(rd1.getProperties().get("_MS.ProcessedByMetricExtractors")).isEqualTo("True");
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd.getProperties().get("_MS.ProcessedByMetricExtractors")).isEqualTo("True");
    assertThat(rdd.getSuccess()).isTrue();

    // TODO (trask): verify rd2

    SmokeTestExtension.assertParentChild(rd1, rdEnvelope1, rddEnvelope, "GET /simple");
    SmokeTestExtension.assertParentChild(
        rdd.getId(), rddEnvelope, rdEnvelope2, "GET /simple", "example.Greeter/SayHello", false);

    verifyRpcClientDurationPreAggregatedMetrics(rpcClientDurationMetrics);
    verifyRpcServerDurationPreAggregatedMetrics(rpcServerMetrics);
  }

  @Test
  @TargetUri("/conversation")
  void doConversationTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /conversation");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/Conversation");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    // auto-collected grpc events are suppressed by exporter because they are noisy
    assertThat(testing.mockedIngestion.getCountForType("MessageData", operationId)).isZero();

    Envelope rddEnvelope = getDependencyEnvelope(rddList, "example.Greeter/Conversation");

    assertThat(rdEnvelope1.getSampleRate()).isNull();
    assertThat(rdEnvelope2.getSampleRate()).isNull();
    assertThat(rddEnvelope.getSampleRate()).isNull();

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertThat(rdd.getTarget()).isEqualTo("localhost:10203");

    assertThat(rd1.getProperties().get("_MS.ProcessedByMetricExtractors")).isEqualTo("True");
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd.getProperties().get("_MS.ProcessedByMetricExtractors")).isEqualTo("True");
    assertThat(rdd.getSuccess()).isTrue();

    // TODO (trask): verify rd2

    SmokeTestExtension.assertParentChild(rd1, rdEnvelope1, rddEnvelope, "GET /conversation");
    SmokeTestExtension.assertParentChild(
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

  private static void verifyRpcClientDurationPreAggregatedMetrics(List<Envelope> metrics) {
    assertThat(metrics.size()).isEqualTo(2);

    // 1st pre-aggregated metric
    Envelope envelope1 = metrics.get(0);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData("client", md1);

    // 2nd pre-aggregated metric
    Envelope envelope2 = metrics.get(1);
    validateTags(envelope2);
    MetricData md2 = (MetricData) ((Data<?>) envelope2.getData()).getBaseData();
    validateMetricData("client", md2);
  }

  private static void verifyRpcServerDurationPreAggregatedMetrics(List<Envelope> metrics) {
    assertThat(metrics.size()).isEqualTo(2);
    // 1st pre-aggregated metric
    Envelope envelope1 = metrics.get(0);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData("server", md1);

    // 2nd pre-aggregated metric
    Envelope envelope2 = metrics.get(1);
    validateTags(envelope2);
    MetricData md2 = (MetricData) ((Data<?>) envelope2.getData()).getBaseData();
    validateMetricData("server", md2);
  }

  private static void validateTags(Envelope envelope) {
    Map<String, String> tags = envelope.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
  }

  private static void validateMetricData(String type, MetricData metricData) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    DataPoint dataPoint = dataPoints.get(0);
    assertThat(dataPoint.getCount()).isEqualTo(1);
    assertThat(dataPoint.getValue()).isGreaterThan(0d).isLessThan(5 * 60 * 1000d); // (0 - 5) min
    assertThat(dataPoint.getMin()).isGreaterThan(0d).isLessThan(5 * 60 * 1000d); // (0 - 5) min
    assertThat(dataPoint.getMin()).isGreaterThan(0d).isLessThan(5 * 60 * 1000d); // (0 - 5) min
    Map<String, String> properties = metricData.getProperties();
    if ("client".equals(type)) {
      assertThat(properties.get("dependency/resultCode")).isNull();
      assertThat(properties.get("_MS.metricId")).isEqualTo("dependencies/duration");
      assertThat(properties.get("dependency/target")).isNotNull();
      assertThat(properties.get("dependency/type")).isEqualTo("grpc");
    } else {
      assertThat(properties.get("_MS.metricId")).isEqualTo("requests/duration");
      assertThat(properties.get("request/resultCode")).isNull();
      assertThat(properties.get("request/success")).isEqualTo("True");
    }
    assertThat(properties.get("operation/synthetic")).isEqualTo("False");
    assertThat(properties.get("cloud/roleInstance")).isEqualTo("testroleinstance");
    assertThat(properties.get("cloud/roleName")).isEqualTo("testrolename");
    assertThat(properties.get("_MS.IsAutocollected")).isEqualTo("True");
  }

  @Environment(JAVA_8)
  static class Java8Test extends GrpcTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends GrpcTest {}

  @Environment(JAVA_11)
  static class Java11Test extends GrpcTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends GrpcTest {}

  @Environment(JAVA_17)
  static class Java17Test extends GrpcTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends GrpcTest {}

  @Environment(JAVA_18)
  static class Java18Test extends GrpcTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends GrpcTest {}

  @Environment(JAVA_19)
  static class Java19Test extends GrpcTest {}
}
