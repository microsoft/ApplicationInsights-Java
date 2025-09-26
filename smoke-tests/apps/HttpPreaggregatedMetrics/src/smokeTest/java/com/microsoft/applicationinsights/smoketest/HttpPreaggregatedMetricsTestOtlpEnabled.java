// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

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
abstract class HttpPreaggregatedMetricsTestOtlpEnabled {

  @RegisterExtension
  static final SmokeTestExtension testing = SmokeTestExtension.builder().useOtlpEndpoint().build();

  @Test
  @TargetUri("/httpUrlConnection")
  void testMsSentToAmw() throws Exception {
    verifyMetrics();
  }

  private void verifyMetrics() throws Exception {
    List<Envelope> clientMetrics =
        testing.mockedIngestion.waitForStandardMetricItems("dependencies/duration", 3);
    List<Envelope> serverMetrics =
        testing.mockedIngestion.waitForStandardMetricItems("requests/duration", 1);

    verifyHttpClientPreAggregatedMetrics(clientMetrics);
    verifyHttpServerPreAggregatedMetrics(serverMetrics);
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
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData(md1);

    // 2nd pre-aggregated metric
    Envelope envelope2 = metrics.get(1);
    MetricData md2 = (MetricData) ((Data<?>) envelope2.getData()).getBaseData();
    validateMetricData(md2);

    // 3rd pre-aggregated metric
    Envelope envelope3 = metrics.get(2);
    MetricData md3 = (MetricData) ((Data<?>) envelope3.getData()).getBaseData();
    validateMetricData(md3);
  }

  private static void verifyHttpServerPreAggregatedMetrics(List<Envelope> metrics) {
    assertThat(metrics.size()).isEqualTo(1);
    // 1st pre-aggregated metric
    Envelope envelope1 = metrics.get(0);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData(md1);
  }

  private static void validateMetricData(MetricData metricData) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);

    // Verify properties - specifically that _MS.SentToAMW is True
    Map<String, String> properties = metricData.getProperties();
    assertThat(properties.get("_MS.SentToAMW")).isEqualTo("True");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(TOMCAT_8_JAVA_23)
  static class Tomcat8Java23Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(TOMCAT_8_JAVA_23_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends HttpPreaggregatedMetricsTestOtlpEnabled {}
}
