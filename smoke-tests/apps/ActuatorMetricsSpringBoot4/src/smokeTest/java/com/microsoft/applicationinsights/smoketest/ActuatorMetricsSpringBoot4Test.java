// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_25;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_25_OPENJ9;
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
abstract class ActuatorMetricsSpringBoot4Test {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void shouldCaptureCustomMetricRegisteredOnSpringMeterRegistry() throws Exception {
    testing.getTelemetry(0);

    List<Envelope> metricItems =
        testing.mockedIngestion.waitForItems(
            ActuatorMetricsSpringBoot4Test::isTargetMetric, 1, 20, TimeUnit.SECONDS);

    MetricData data = (MetricData) ((Data<?>) metricItems.get(0).getData()).getBaseData();
    List<DataPoint> points = data.getMetrics();

    assertThat(points)
        .anySatisfy(point -> assertThat(point.getName()).isEqualTo("demo_requests_total"));
    assertThat(data.getProperties()).containsEntry("endpoint", "test");
  }

  static boolean isTargetMetric(Envelope input) {
    if (!input.getData().getBaseType().equals("MetricData")) {
      return false;
    }
    MetricData data = (MetricData) ((Data<?>) input.getData()).getBaseData();
    for (DataPoint point : data.getMetrics()) {
      if ("demo_requests_total".equals(point.getName()) && point.getValue() >= 1) {
        return true;
      }
    }
    return false;
  }

  @Environment(JAVA_17)
  static class Java17Test extends ActuatorMetricsSpringBoot4Test {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends ActuatorMetricsSpringBoot4Test {}

  @Environment(JAVA_21)
  static class Java21Test extends ActuatorMetricsSpringBoot4Test {}

  @Environment(JAVA_21_OPENJ9)
  static class Java21OpenJ9Test extends ActuatorMetricsSpringBoot4Test {}

  @Environment(JAVA_25)
  static class Java25Test extends ActuatorMetricsSpringBoot4Test {}

  @Environment(JAVA_25_OPENJ9)
  static class Java25OpenJ9Test extends ActuatorMetricsSpringBoot4Test {}
}
