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
abstract class HttpPreaggregatedMetricsSmokeTest {

  @RegisterExtension static final SmokeTestExtension testing = new SmokeTestExtension();

  @Test
  @TargetUri("/httpUrlConnection")
  void testHttpUrlConnection() throws Exception {
    verify();
  }

  private static void verify() throws Exception {
    verify("https://mock.codes/200?q=spaces%20test");
  }

  private static void verify(String successUrlWithQueryString) throws Exception {
    List<Envelope> clientMetrics =
        testing.mockedIngestion.waitForItems(
            SmokeTestExtension.getMetricPredicate("http.client.duration"), 3, 40, TimeUnit.SECONDS);
    List<Envelope> serverMetrics =
        testing.mockedIngestion.waitForItems(
            SmokeTestExtension.getMetricPredicate("http.server.duration"), 2, 40, TimeUnit.SECONDS);

    verifyHttpClientPreAggregatedMetrics(clientMetrics);
    verifyHttpServerPreAggregatedMetrics(serverMetrics);
  }

  private static void verifyHttpClientPreAggregatedMetrics(List<Envelope> metrics)
      throws Exception {
    assertThat(metrics.size()).isEqualTo(3);
    // sort metrics based on result code
    metrics.sort(
        Comparator.comparing(
            obj -> {
              MetricData metricData = (MetricData) ((Data<?>) obj.getData()).getBaseData();
              return metricData.getProperties().get("request/resultCode");
            }));

    // 1st pre-aggregated metric
    Envelope envelope1 = metrics.get(0);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData(md1, "200");

    // 2nd pre-aggregated metric
    Envelope envelope2 = metrics.get(1);
    validateTags(envelope2);
    MetricData md2 = (MetricData) ((Data<?>) envelope2.getData()).getBaseData();
    validateMetricData(md2, "404");

    // 3rd pre-aggregated metric
    Envelope envelope3 = metrics.get(2);
    validateTags(envelope3);
    MetricData md3 = (MetricData) ((Data<?>) envelope3.getData()).getBaseData();
    validateMetricData(md3, "500");
  }

  private static void verifyHttpServerPreAggregatedMetrics(List<Envelope> metrics)
      throws Exception {
    assertThat(metrics.size()).isEqualTo(2);
    // 1st pre-aggregated metric
    Envelope envelope1 = metrics.get(0);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData(md1, "200");

    // 2nd pre-aggregated metric
    Envelope envelope2 = metrics.get(1);
    validateTags(envelope2);
    MetricData md2 = (MetricData) ((Data<?>) envelope2.getData()).getBaseData();
    validateMetricData(md2, "200");
  }

  private static void validateTags(Envelope envelope) {
    Map<String, String> tags = envelope.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
  }

  private static void validateMetricData(MetricData metricData, String resultCode) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    DataPoint dataPoint = dataPoints.get(0);
    assertThat(dataPoint.getCount()).isEqualTo(1);
    assertThat(dataPoint.getValue()).isGreaterThan(0d).isLessThan(5 * 60 * 1000d); // (0 - 5) min
    assertThat(dataPoint.getMin()).isGreaterThan(0d).isLessThan(5 * 60 * 1000d); // (0 - 5) min
    assertThat(dataPoint.getMin()).isGreaterThan(0d).isLessThan(5 * 60 * 1000d); // (0 - 5) min
    Map<String, String> properties = metricData.getProperties();
    assertThat(properties.get("request/resultCode")).isEqualTo(resultCode);

    double value = metricData.getMetrics().get(0).getValue();
    assertThat(properties.get("request/performanceBucket")).isEqualTo(getPerformanceBucket(value));
    if ("200".equals(resultCode)) {
      assertThat(properties.get("request/success")).isEqualTo("True");
    } else {
      assertThat(properties.get("request/success")).isEqualTo("False");
    }
    assertThat(properties.get("operation/synthetic")).isEqualTo("False");
    assertThat(properties.get("_MS.metricId")).isEqualTo("requests/duration");
    assertThat(properties.get("_MS.ProcessedByMetricExtractors")).isEqualTo("True");
    assertThat(properties.get("cloud/roleInstance")).isEqualTo("testroleinstance");
    assertThat(properties.get("cloud/roleName")).isEqualTo("testrolename");
    assertThat(properties.get("_MS.IsAutocollected")).isEqualTo("True");
  }

  private static String getPerformanceBucket(double duration) {
    return DurationBucketizer.getPerformanceBucket(duration);
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends HttpPreaggregatedMetricsSmokeTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends HttpPreaggregatedMetricsSmokeTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends HttpPreaggregatedMetricsSmokeTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends HttpPreaggregatedMetricsSmokeTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends HttpPreaggregatedMetricsSmokeTest {}

  @Environment(TOMCAT_8_JAVA_18)
  static class Tomcat8Java18Test extends HttpPreaggregatedMetricsSmokeTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends HttpPreaggregatedMetricsSmokeTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends HttpPreaggregatedMetricsSmokeTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends HttpPreaggregatedMetricsSmokeTest {}
}
