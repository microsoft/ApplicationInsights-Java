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

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class OpenTelemetryMetricTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

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
    List<Envelope> metrics = testing.mockedIngestion.waitForMetricItems(name, 1);

    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("GET /OpenTelemetryMetric/" + name);

    Envelope envelope = metrics.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(envelope.getSampleRate()).isNull();

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
    List<Envelope> metrics = testing.mockedIngestion.waitForMetricItems(name, 1);

    Envelope rdEnvelope = rdList.get(0);
    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    assertThat(rd.getName()).isEqualTo("GET /OpenTelemetryMetric/" + name);

    Envelope envelope = metrics.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(envelope.getSampleRate()).isNull();

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
    List<Envelope> metrics = testing.mockedIngestion.waitForMetricItems(name, 3);

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
    Envelope envelope2 = metrics.get(1);
    Envelope envelope3 = metrics.get(2);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(envelope1.getSampleRate()).isNull();
    assertThat(envelope2.getSampleRate()).isNull();
    assertThat(envelope3.getSampleRate()).isNull();

    // validate tags
    Map<String, String> tags1 = envelope1.getTags();
    assertThat(tags1.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags1).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags1).containsEntry("ai.cloud.role", "testrolename");
    assertThat(tags1).containsEntry("ai.application.ver", "123");

    // validate base data
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    List<DataPoint> dataPointList1 = md1.getMetrics();
    assertThat(dataPointList1).hasSize(1);
    DataPoint dp1 = dataPointList1.get(0);
    assertThat(dp1.getValue()).isEqualTo(2.0);
    assertThat(dp1.getName()).isEqualTo(name);

    // validate custom dimension
    Map<String, String> properties1 = md1.getProperties();
    assertThat(properties1).containsEntry("tag1", "abc");
    assertThat(properties1).containsEntry("tag2", "def");
    assertThat(properties1).containsEntry("name", "apple");
    assertThat(properties1).containsEntry("color", "green");

    // validate tags
    Map<String, String> tags2 = envelope2.getTags();
    assertThat(tags2.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags2).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags2).containsEntry("ai.cloud.role", "testrolename");
    assertThat(tags2).containsEntry("ai.application.ver", "123");

    // validate base data
    MetricData md2 = (MetricData) ((Data<?>) envelope2.getData()).getBaseData();
    List<DataPoint> dataPointList2 = md2.getMetrics();
    assertThat(dataPointList2).hasSize(1);
    DataPoint dp2 = dataPointList2.get(0);
    assertThat(dp2.getValue()).isEqualTo(6.0);
    assertThat(dp2.getName()).isEqualTo(name);

    // validate custom dimension
    Map<String, String> properties2 = md2.getProperties();
    assertThat(properties2).containsEntry("tag1", "abc");
    assertThat(properties2).containsEntry("tag2", "def");
    assertThat(properties2).containsEntry("name", "apple");
    assertThat(properties2).containsEntry("color", "red");

    // validate tags
    Map<String, String> tags3 = envelope3.getTags();
    assertThat(tags3.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags3).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags3).containsEntry("ai.cloud.role", "testrolename");
    assertThat(tags3).containsEntry("ai.application.ver", "123");

    // validate base data
    MetricData md3 = (MetricData) ((Data<?>) envelope3.getData()).getBaseData();
    List<DataPoint> dataPointList3 = md3.getMetrics();
    assertThat(dataPointList3).hasSize(1);
    DataPoint dp3 = dataPointList3.get(0);
    assertThat(dp3.getValue()).isEqualTo(7.0);
    assertThat(dp3.getName()).isEqualTo(name);

    // validate custom dimension
    Map<String, String> properties3 = md3.getProperties();
    assertThat(properties3).containsEntry("tag1", "abc");
    assertThat(properties3).containsEntry("tag2", "def");
    assertThat(properties3).containsEntry("name", "lemon");
    assertThat(properties3).containsEntry("color", "yellow");
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

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends OpenTelemetryMetricTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends OpenTelemetryMetricTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends OpenTelemetryMetricTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends OpenTelemetryMetricTest {}
}
