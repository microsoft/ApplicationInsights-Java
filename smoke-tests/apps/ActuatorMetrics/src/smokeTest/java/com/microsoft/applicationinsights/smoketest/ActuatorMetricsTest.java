// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class ActuatorMetricsTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    testing.getTelemetry(0);

    List<Envelope> metricItems =
        testing.mockedIngestion.waitForItems(
            ActuatorMetricsTest::isMicrometerMetric, 1, 10, TimeUnit.SECONDS);

    MetricData data = (MetricData) ((Data<?>) metricItems.get(0).getData()).getBaseData();
    List<DataPoint> points = data.getMetrics();
    assertThat(points).hasSize(1);

    DataPoint point = points.get(0);

    // these were verified above in Predicate also
    assertThat(point.getCount()).isEqualTo(1);
    assertThat(point.getName()).isEqualTo("http_server_requests");

    // running with micrometer 1.5+ and so using upstream micrometer instrumentation
    assertThat(point.getMin()).isNotNull();

    assertThat(point.getMax()).isNotNull();
    assertThat(point.getStdDev()).isNull();
  }

  static boolean isMicrometerMetric(Envelope input) {
    if (!input.getData().getBaseType().equals("MetricData")) {
      return false;
    }
    MetricData data = (MetricData) ((Data<?>) input.getData()).getBaseData();
    if (!"/test".equals(data.getProperties().get("uri"))) {
      return false;
    }
    for (DataPoint point : data.getMetrics()) {
      if (point.getName().equals("http_server_requests") && point.getCount() == 1) {
        return true;
      }
    }
    return false;
  }

  @Environment(JAVA_8)
  static class Java8Test extends ActuatorMetricsTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends ActuatorMetricsTest {}

  @Environment(JAVA_11)
  static class Java11Test extends ActuatorMetricsTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends ActuatorMetricsTest {}

  @Environment(JAVA_17)
  static class Java17Test extends ActuatorMetricsTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends ActuatorMetricsTest {}

  @Environment(JAVA_20)
  static class JavaLatestTest extends ActuatorMetricsTest {}

  @Environment(JAVA_20_OPENJ9)
  static class JavaLatestOpenJ9Test extends ActuatorMetricsTest {}

  @Environment(JAVA_21)
  static class JavaPrereleaseTest extends ActuatorMetricsTest {}
}
