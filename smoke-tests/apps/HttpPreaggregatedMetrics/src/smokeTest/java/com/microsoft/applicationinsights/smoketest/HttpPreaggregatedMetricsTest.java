// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class HttpPreaggregatedMetricsTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/httpUrlConnection")
  void testHttpUrlConnection() throws Exception {
    testHttpUrlConnectionAndSynthetic(false);
  }

  @Test
  @TargetUri(value = "/httpUrlConnection", userAgent = "AlwaysOn")
  void testHttpUrlConnectionAndSynthetic() throws Exception {
    testHttpUrlConnectionAndSynthetic(true);
  }

  private void testHttpUrlConnectionAndSynthetic(boolean synthetic) throws Exception {
    verifyHttpclientRequestsAndDependencies(
        "http://host.testcontainers.internal:6060/mock/200?q=spaces%20test");

    List<Envelope> clientMetrics =
        testing.mockedIngestion.waitForStandardMetricItems("dependencies/duration", 3);
    List<Envelope> serverMetrics =
        testing.mockedIngestion.waitForStandardMetricItems("requests/duration", 1);

    verifyHttpClientPreAggregatedMetrics(clientMetrics);
    verifyHttpServerPreAggregatedMetrics(serverMetrics, synthetic);
  }

  private static void verifyHttpclientRequestsAndDependencies(String successUrlWithQueryString)
      throws Exception {
    Telemetry telemetry = testing.getTelemetry(3);

    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rdd1.getData()).isEqualTo(successUrlWithQueryString);
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rdEnvelope.getSampleRate()).isNull();
    assertThat(telemetry.rdd1.getName()).isEqualTo("GET /mock/200");
    assertThat(telemetry.rdd1.getType()).isEqualTo("Http");
    assertThat(telemetry.rdd1.getTarget()).isEqualTo("host.testcontainers.internal:6060");
    assertThat(telemetry.rdd1.getResultCode()).isEqualTo("200");
    assertThat(telemetry.rdd1.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rdd1.getSuccess()).isTrue();
    assertThat(telemetry.rddEnvelope1.getSampleRate()).isNull();

    assertThat(telemetry.rdd2.getName()).isEqualTo("GET /mock/404");
    assertThat(telemetry.rdd2.getData())
        .isEqualTo("http://host.testcontainers.internal:6060/mock/404");
    assertThat(telemetry.rdd2.getType()).isEqualTo("Http");
    assertThat(telemetry.rdd2.getTarget()).isEqualTo("host.testcontainers.internal:6060");
    assertThat(telemetry.rdd2.getResultCode()).isEqualTo("404");
    assertThat(telemetry.rdd2.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rdd2.getSuccess()).isFalse();
    assertThat(telemetry.rddEnvelope2.getSampleRate()).isNull();

    assertThat(telemetry.rdd3.getName()).isEqualTo("GET /mock/500");
    assertThat(telemetry.rdd3.getData())
        .isEqualTo("http://host.testcontainers.internal:6060/mock/500");
    assertThat(telemetry.rdd3.getType()).isEqualTo("Http");
    assertThat(telemetry.rdd3.getTarget()).isEqualTo("host.testcontainers.internal:6060");
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

  private static void verifyHttpClientPreAggregatedMetrics(List<Envelope> metrics) {
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
    validateMetricData("client", md1, "200", false);

    // 2nd pre-aggregated metric
    Envelope envelope2 = metrics.get(1);
    validateTags(envelope2);
    MetricData md2 = (MetricData) ((Data<?>) envelope2.getData()).getBaseData();
    validateMetricData("client", md2, "404", false);

    // 3rd pre-aggregated metric
    Envelope envelope3 = metrics.get(2);
    validateTags(envelope3);
    MetricData md3 = (MetricData) ((Data<?>) envelope3.getData()).getBaseData();
    validateMetricData("client", md3, "500", false);
  }

  private static void verifyHttpServerPreAggregatedMetrics(
      List<Envelope> metrics, boolean synthetic) {
    assertThat(metrics.size()).isEqualTo(1);
    // 1st pre-aggregated metric
    Envelope envelope1 = metrics.get(0);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData("server", md1, "200", synthetic);
  }

  private static void validateTags(Envelope envelope) {
    Map<String, String> tags = envelope.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
  }

  private static void validateMetricData(
      String type, MetricData metricData, String resultCode, boolean synthetic) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    DataPoint dataPoint = dataPoints.get(0);
    assertThat(dataPoint.getCount()).isEqualTo(1);
    double greaterThan = type.equals("server") ? 200.0f : 0.0f;
    assertThat(dataPoint.getValue()).isGreaterThan(greaterThan).isLessThan(60 * 1000.0);
    assertThat(dataPoint.getMin()).isGreaterThan(greaterThan).isLessThan(60 * 1000.0);
    assertThat(dataPoint.getMax()).isGreaterThan(greaterThan).isLessThan(60 * 1000.0);
    String expectedSuccess = Integer.parseInt(resultCode) < 400 ? "True" : "False";
    Map<String, String> properties = metricData.getProperties();
    if ("client".equals(type)) {
      assertThat(properties).hasSize(10);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("dependencies/duration");
      assertThat(properties.get("dependency/resultCode")).isEqualTo(resultCode);
      assertThat(properties.get("Dependency.Success")).isEqualTo(expectedSuccess);
      assertThat(properties.get("dependency/target"))
          .isEqualTo("host.testcontainers.internal:6060");
      assertThat(properties.get("Dependency.Type")).isEqualTo("Http");
      assertThat(properties.get("operation/synthetic")).isEqualTo("False");
    } else {
      assertThat(properties).hasSize(8);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("requests/duration");
      assertThat(properties.get("request/resultCode")).isEqualTo(resultCode);
      assertThat(properties.get("Request.Success")).isEqualTo(expectedSuccess);
      assertThat(properties.get("operation/synthetic")).isEqualTo(synthetic ? "True" : "False");
    }
    assertThat(properties.get("cloud/roleInstance")).isEqualTo("testroleinstance");
    assertThat(properties.get("cloud/roleName")).isEqualTo("testrolename");
    assertThat(properties.get("_MS.IsAutocollected")).isEqualTo("True");
    assertThat(properties.get("_MS.SentToAMW")).isEqualTo("False");
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

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends HttpPreaggregatedMetricsTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends HttpPreaggregatedMetricsTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends HttpPreaggregatedMetricsTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends HttpPreaggregatedMetricsTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends HttpPreaggregatedMetricsTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends HttpPreaggregatedMetricsTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends HttpPreaggregatedMetricsTest {}
}
