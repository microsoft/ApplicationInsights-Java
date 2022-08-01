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
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class OpenTelemetryMetricTest {

  @RegisterExtension static final SmokeTestExtension testing = new SmokeTestExtension();

  @Test
  @TargetUri("/trackDoubleCounterMetric")
  void trackDoubleCounterMetric() throws Exception {
    validateCounterMetric("trackDoubleCounterMetric");
  }

  @Test
  @TargetUri("/trackLongCounterMetric")
  void trackLongCounterMetric() throws Exception {
    validateCounterMetric("trackLongCounterMetric");
  }

  @Test
  @TargetUri("/trackDoubleGaugeMetric")
  void trackDoubleGaugeMetric() throws Exception {
    validateGaugeMetric("trackDoubleGaugeMetric");
  }

  @Test
  @TargetUri("/trackLongGaugeMetric")
  void trackLongGaugeMetric() throws Exception {
    validateGaugeMetric("trackLongGaugeMetric");
  }

  @Test
  @TargetUri("/trackDoubleHistogramMetric")
  void trackDoubleHistogramMetric() throws Exception {
    validateHistogramMetric("trackDoubleHistogramMetric");
  }

  @Test
  @TargetUri("/trackLongHistogramMetric")
  void trackLongHistogramMetric() throws Exception {
    validateHistogramMetric("trackLongHistogramMetric");
  }

  private void validateHistogramMetric(String name) throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> metrics =
        testing.mockedIngestion.waitForItems(
            SmokeTestExtension.getMetricPredicate(name), 1, 40, TimeUnit.SECONDS);
    assertThat(metrics).hasSize(1);

    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("GET /OpenTelemetryMetric/" + name);

    Envelope envelope = metrics.get(0);

    // validate tags
    Map<String, String> tags = envelope.getTags();
    assertThat(tags).containsKey("ai.internal.sdkVersion");
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
    assertThat(tags).containsEntry("ai.application.ver", "123");

    // validate base data
    MetricData md = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
    List<DataPoint> dataPointList = md.getMetrics();
    assertThat(dataPointList).hasSize(1);
    DataPoint dp = dataPointList.get(0);
    assertThat(dp.getValue()).isEqualTo(456);
    assertThat(dp.getName()).isEqualTo(name);
    assertThat(dp.getCount()).isEqualTo(1);
    assertThat(dp.getMin()).isEqualTo(456);
    assertThat(dp.getMax()).isEqualTo(456);

    // validate custom dimension
    Map<String, String> properties = md.getProperties();
    assertThat(properties).containsEntry("tag1", "abc");
    assertThat(properties).containsEntry("tag2", "def");
  }

  private void validateGaugeMetric(String name) throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> metrics =
        testing.mockedIngestion.waitForItems(
            SmokeTestExtension.getMetricPredicate(name), 1, 40, TimeUnit.SECONDS);
    assertThat(metrics).hasSize(1);

    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("GET /OpenTelemetryMetric/" + name);

    Envelope envelope = metrics.get(0);

    // validate tags
    Map<String, String> tags = envelope.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
    assertThat(tags).containsEntry("ai.application.ver", "123");

    // validate base data
    MetricData md = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
    List<DataPoint> dataPointList = md.getMetrics();
    assertThat(dataPointList).hasSize(1);
    DataPoint dp = dataPointList.get(0);
    assertThat(dp.getValue()).isEqualTo(10);
    assertThat(dp.getName()).isEqualTo(name);

    // validate custom dimension
    Map<String, String> properties = md.getProperties();
    assertThat(properties).containsEntry("tag1", "abc");
    assertThat(properties).containsEntry("tag2", "def");
    assertThat(properties).containsEntry("thing1", "thing2");
  }

  private void validateCounterMetric(String name) throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> metrics =
        testing.mockedIngestion.waitForItems(
            SmokeTestExtension.getMetricPredicate(name), 3, 40, TimeUnit.SECONDS);
    assertThat(metrics).hasSize(3);

    metrics.sort(
        Comparator.comparing(
            obj -> {
              MetricData metricData = (MetricData) ((Data<?>) obj.getData()).getBaseData();
              List<DataPoint> dataPointList = metricData.getMetrics();
              DataPoint dataPoint = dataPointList.get(0);
              return dataPoint.getValue();
            }));

    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("GET /OpenTelemetryMetric/" + name);

    // validate 1st metric
    Envelope envelope1 = metrics.get(0);

    // validate tags
    Map<String, String> tags = envelope1.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
    assertThat(tags).containsEntry("ai.application.ver", "123");

    // validate base data
    MetricData md = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    List<DataPoint> dataPointList = md.getMetrics();
    assertThat(dataPointList).hasSize(1);
    DataPoint dp = dataPointList.get(0);
    assertThat(dp.getValue()).isEqualTo(2.0);
    assertThat(dp.getName()).isEqualTo(name);

    // validate custom dimension
    Map<String, String> properties = md.getProperties();
    assertThat(properties).containsEntry("tag1", "abc");
    assertThat(properties).containsEntry("tag2", "def");
    assertThat(properties).containsEntry("name", "apple");
    assertThat(properties).containsEntry("color", "green");

    // validate 2nd metric
    Envelope envelope2 = metrics.get(1);

    // validate tags
    tags = envelope2.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
    assertThat(tags).containsEntry("ai.application.ver", "123");

    // validate base data
    md = (MetricData) ((Data<?>) envelope2.getData()).getBaseData();
    dataPointList = md.getMetrics();
    assertThat(dataPointList).hasSize(1);
    dp = dataPointList.get(0);
    assertThat(dp.getValue()).isEqualTo(6.0);
    assertThat(dp.getName()).isEqualTo(name);

    // validate custom dimension
    properties = md.getProperties();
    assertThat(properties).containsEntry("tag1", "abc");
    assertThat(properties).containsEntry("tag2", "def");
    assertThat(properties).containsEntry("name", "apple");
    assertThat(properties).containsEntry("color", "red");

    // validate 3rd metric
    Envelope envelope3 = metrics.get(2);

    // validate tags
    tags = envelope3.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
    assertThat(tags).containsEntry("ai.application.ver", "123");

    // validate base data
    md = (MetricData) ((Data<?>) envelope3.getData()).getBaseData();
    dataPointList = md.getMetrics();
    assertThat(dataPointList).hasSize(1);
    dp = dataPointList.get(0);
    assertThat(dp.getValue()).isEqualTo(7.0);
    assertThat(dp.getName()).isEqualTo(name);

    // validate custom dimension
    properties = md.getProperties();
    assertThat(properties).containsEntry("tag1", "abc");
    assertThat(properties).containsEntry("tag2", "def");
    assertThat(properties).containsEntry("name", "lemon");
    assertThat(properties).containsEntry("color", "yellow");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends OpenTelemetryMetricTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends OpenTelemetryMetricTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends OpenTelemetryMetricTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends OpenTelemetryMetricTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends OpenTelemetryMetricTest {}

  @Environment(TOMCAT_8_JAVA_18)
  static class Tomcat8Java18Test extends OpenTelemetryMetricTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends OpenTelemetryMetricTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends OpenTelemetryMetricTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends OpenTelemetryMetricTest {}
}
