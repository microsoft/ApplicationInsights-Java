// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_18_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class MicrometerTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /test");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/test");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    List<Envelope> metricItems =
        testing.mockedIngestion.waitForItems(
            MicrometerTest::isMicrometerMetricWithValueOne, 1, 10, TimeUnit.SECONDS);

    MetricData data = (MetricData) ((Data<?>) metricItems.get(0).getData()).getBaseData();
    List<DataPoint> points = data.getMetrics();
    assertThat(points).hasSize(1);

    DataPoint point = points.get(0);

    assertThat(point.getValue()).isEqualTo(1); // (this was verified above in Predicate also)
    assertThat(point.getName()).isEqualTo("test_counter");
    assertThat(point.getCount()).isNull();
    assertThat(point.getMin()).isNull();
    assertThat(point.getMax()).isNull();
    assertThat(point.getStdDev()).isNull();
    assertThat(data.getProperties()).hasSize(1);
    assertThat(data.getProperties()).containsEntry("tag1", "value1");
  }

  static boolean isMicrometerMetricWithValueOne(Envelope input) {
    if (!input.getData().getBaseType().equals("MetricData")) {
      return false;
    }
    MetricData data = (MetricData) ((Data<?>) input.getData()).getBaseData();
    for (DataPoint point : data.getMetrics()) {
      if (point.getName().contains("test_counter") && point.getValue() == 1) {
        return true;
      }
    }
    return false;
  }

  @Environment(JAVA_8)
  static class Java8Test extends MicrometerTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends MicrometerTest {}

  @Environment(JAVA_11)
  static class Java11Test extends MicrometerTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends MicrometerTest {}

  @Environment(JAVA_17)
  static class Java17Test extends MicrometerTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends MicrometerTest {}

  @Environment(JAVA_19)
  static class Java18Test extends MicrometerTest {}

  @Environment(JAVA_18_OPENJ9)
  static class Java18OpenJ9Test extends MicrometerTest {}

  @Environment(JAVA_20)
  static class Java19Test extends MicrometerTest {}
}
