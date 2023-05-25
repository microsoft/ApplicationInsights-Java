// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class DetectUnexpectedOtelMetricsTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/app2")
  void testApp2() throws Exception {
    testApp("app2", "12345678-0000-0000-0000-0FEEDDADBEEF");
  }

  @Test
  @TargetUri("/app3")
  void testApp3() throws Exception {
    testApp("app3", "87654321-0000-0000-0000-0FEEDDADBEEF");
  }

  private static void testApp(String roleName, String iKey) throws Exception {
    List<Envelope> clientMetrics =
        testing.mockedIngestion.waitForMetricItems("http.client.duration", 1);
    List<Envelope> serverMetrics =
        testing.mockedIngestion.waitForMetricItems("http.server.duration", 1);

    verifyHttpClientPreAggregatedMetrics(clientMetrics, roleName, iKey);
    verifyHttpServerPreAggregatedMetrics(serverMetrics, roleName, iKey);

    // no other otel metrics other than EXPECTED_OTEL_METRICS, expect an TimeoutException being
    // thrown
    assertThatThrownBy(
            () ->
                testing.mockedIngestion.waitForItemsUnexpectedOtelMetric(
                    "MetricData", envelope -> true))
        .isInstanceOf(TimeoutException.class);
  }

  private static void verifyHttpClientPreAggregatedMetrics(
      List<Envelope> metrics, String roleName, String iKey) {
    assertThat(metrics.size()).isEqualTo(1);

    Envelope envelope = metrics.get(0);
    assertThat(envelope.getIKey()).isEqualTo(iKey);
    validateTags(envelope, roleName);
    MetricData md1 = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
    validateMetricData("client", md1, "200", roleName);
  }

  private static void verifyHttpServerPreAggregatedMetrics(
      List<Envelope> metrics, String roleName, String iKey) {
    assertThat(metrics.size()).isEqualTo(1);

    Envelope envelope = metrics.get(0);
    assertThat(envelope.getIKey()).isEqualTo(iKey);
    validateTags(envelope, roleName);
    MetricData md1 = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
    validateMetricData("server", md1, "200", roleName);
  }

  private static void validateTags(Envelope envelope, String roleName) {
    Map<String, String> tags = envelope.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", roleName);
  }

  private static void validateMetricData(
      String type, MetricData metricData, String resultCode, String roleName) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    DataPoint dataPoint = dataPoints.get(0);
    assertThat(dataPoint.getCount()).isEqualTo(1);
    assertThat(dataPoint.getValue()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    assertThat(dataPoint.getMin()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    assertThat(dataPoint.getMax()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    Map<String, String> properties = metricData.getProperties();
    String expectedSuccess = "200".equals(resultCode) ? "True" : "False";
    if ("client".equals(type)) {
      assertThat(properties).hasSize(9);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("dependencies/duration");
      assertThat(properties.get("dependency/resultCode")).isEqualTo(resultCode);
      assertThat(properties.get("Dependency.Success")).isEqualTo(expectedSuccess);
      assertThat(properties.get("dependency/target")).isEqualTo("mock.codes");
      assertThat(properties.get("Dependency.Type")).isEqualTo("Http");
    } else {
      assertThat(properties).hasSize(7);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("requests/duration");
      assertThat(properties.get("request/resultCode")).isEqualTo(resultCode);
      assertThat(properties.get("Request.Success")).isEqualTo(expectedSuccess);
    }
    assertThat(properties.get("operation/synthetic")).isEqualTo("False");
    assertThat(properties.get("cloud/roleInstance")).isEqualTo("testroleinstance");
    assertThat(properties.get("cloud/roleName")).isEqualTo(roleName);
    assertThat(properties.get("_MS.IsAutocollected")).isEqualTo("True");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends DetectUnexpectedOtelMetricsTest {}
}
