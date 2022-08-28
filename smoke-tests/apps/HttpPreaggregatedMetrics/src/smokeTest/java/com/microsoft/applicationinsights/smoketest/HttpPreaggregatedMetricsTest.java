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

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_18;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class HttpPreaggregatedMetricsTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/httpUrlConnection")
  void testHttpUrlConnection() throws Exception {
    verifyHttpclientRequestsAndDependencies("https://mock.codes/200?q=spaces%20test");

    List<Envelope> clientMetrics =
        testing.mockedIngestion.waitForItems(
            SmokeTestExtension.getMetricPredicate("http.client.duration"), 3, 40, TimeUnit.SECONDS);
    List<Envelope> serverMetrics =
        testing.mockedIngestion.waitForItems(
            SmokeTestExtension.getMetricPredicate("http.server.duration"), 1, 40, TimeUnit.SECONDS);

    verifyHttpClientPreAggregatedMetrics(clientMetrics);
    verifyHttpServerPreAggregatedMetrics(serverMetrics);
  }

  private static void verifyHttpclientRequestsAndDependencies(String successUrlWithQueryString)
      throws Exception {
    Telemetry telemetry = testing.getTelemetry(3);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rdd1.getData()).isEqualTo(successUrlWithQueryString);
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rdEnvelope.getSampleRate()).isNull();
    assertThat(telemetry.rdd1.getName()).isEqualTo("GET /200");
    assertThat(telemetry.rdd1.getType()).isEqualTo("Http");
    assertThat(telemetry.rdd1.getTarget()).isEqualTo("mock.codes");
    assertThat(telemetry.rdd1.getResultCode()).isEqualTo("200");
    assertThat(telemetry.rdd1.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rdd1.getSuccess()).isTrue();
    assertThat(telemetry.rddEnvelope1.getSampleRate()).isNull();

    assertThat(telemetry.rdd2.getName()).isEqualTo("GET /404");
    assertThat(telemetry.rdd2.getData()).isEqualTo("https://mock.codes/404");
    assertThat(telemetry.rdd2.getType()).isEqualTo("Http");
    assertThat(telemetry.rdd2.getTarget()).isEqualTo("mock.codes");
    assertThat(telemetry.rdd2.getResultCode()).isEqualTo("404");
    assertThat(telemetry.rdd2.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rdd2.getSuccess()).isFalse();
    assertThat(telemetry.rddEnvelope2.getSampleRate()).isNull();

    assertThat(telemetry.rdd3.getName()).isEqualTo("GET /500");
    assertThat(telemetry.rdd3.getData()).isEqualTo("https://mock.codes/500");
    assertThat(telemetry.rdd3.getType()).isEqualTo("Http");
    assertThat(telemetry.rdd3.getTarget()).isEqualTo("mock.codes");
    assertThat(telemetry.rdd3.getResultCode()).isEqualTo("500");
    assertThat(telemetry.rdd3.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rdd3.getSuccess()).isFalse();
    assertThat(telemetry.rddEnvelope3.getSampleRate()).isNull();

    SmokeTestExtension.assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /HttpPreaggregatedMetrics/*");
    SmokeTestExtension.assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope2,
        "GET /HttpPreaggregatedMetrics/*");
    SmokeTestExtension.assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope3,
        "GET /HttpPreaggregatedMetrics/*");
  }

  private static void verifyHttpClientPreAggregatedMetrics(List<Envelope> metrics)
      throws Exception {
    assertThat(metrics.size()).isEqualTo(3);
    // sort metrics based on result code
    metrics.sort(
        Comparator.comparing(
            obj -> {
              MetricData metricData = (MetricData) ((Data<?>) obj.getData()).getBaseData();
              return metricData.getProperties().get("dependency/resultCode");
            }));

    // 1st pre-aggregated metric
    Envelope envelope1 = metrics.get(0);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData("client", md1, "200");

    // 2nd pre-aggregated metric
    Envelope envelope2 = metrics.get(1);
    validateTags(envelope2);
    MetricData md2 = (MetricData) ((Data<?>) envelope2.getData()).getBaseData();
    validateMetricData("client", md2, "404");

    // 3rd pre-aggregated metric
    Envelope envelope3 = metrics.get(2);
    validateTags(envelope3);
    MetricData md3 = (MetricData) ((Data<?>) envelope3.getData()).getBaseData();
    validateMetricData("client", md3, "500");
  }

  private static void verifyHttpServerPreAggregatedMetrics(List<Envelope> metrics) {
    assertThat(metrics.size()).isEqualTo(1);
    // 1st pre-aggregated metric
    Envelope envelope1 = metrics.get(0);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData("server", md1, "200");
  }

  private static void validateTags(Envelope envelope) {
    Map<String, String> tags = envelope.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
  }

  private static void validateMetricData(String type, MetricData metricData, String resultCode) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    DataPoint dataPoint = dataPoints.get(0);
    assertThat(dataPoint.getCount()).isEqualTo(1);
    assertThat(dataPoint.getValue()).isGreaterThan(0d).isLessThan(5 * 60 * 1000d); // (0 - 5) min
    assertThat(dataPoint.getMin()).isGreaterThan(0d).isLessThan(5 * 60 * 1000d); // (0 - 5) min
    assertThat(dataPoint.getMax()).isGreaterThan(0d).isLessThan(5 * 60 * 1000d); // (0 - 5) min
    Map<String, String> properties = metricData.getProperties();
    String expectedSuccess = "200".equals(resultCode) ? "True" : "False";
    if ("client".equals(type)) {
      assertThat(properties).hasSize(9);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("dependencies/duration");
      assertThat(properties.get("dependency/resultCode")).isEqualTo(resultCode);
      assertThat(properties.get("dependency/success")).isEqualTo(expectedSuccess);
      assertThat(properties.get("dependency/target")).isEqualTo("mock.codes");
      assertThat(properties.get("dependency/type")).isEqualTo("Http");
    } else {
      assertThat(properties).hasSize(7);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("requests/duration");
      assertThat(properties.get("request/resultCode")).isEqualTo(resultCode);
      assertThat(properties.get("request/success")).isEqualTo(expectedSuccess);
    }
    assertThat(properties.get("operation/synthetic")).isEqualTo("False");
    assertThat(properties.get("cloud/roleInstance")).isEqualTo("testroleinstance");
    assertThat(properties.get("cloud/roleName")).isEqualTo("testrolename");
    assertThat(properties.get("_MS.IsAutocollected")).isEqualTo("True");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends HttpPreaggregatedMetricsTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends HttpPreaggregatedMetricsTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends HttpPreaggregatedMetricsTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends HttpPreaggregatedMetricsTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends HttpPreaggregatedMetricsTest {}

  @Environment(TOMCAT_8_JAVA_18)
  static class Tomcat8Java18Test extends HttpPreaggregatedMetricsTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends HttpPreaggregatedMetricsTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends HttpPreaggregatedMetricsTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends HttpPreaggregatedMetricsTest {}
}
