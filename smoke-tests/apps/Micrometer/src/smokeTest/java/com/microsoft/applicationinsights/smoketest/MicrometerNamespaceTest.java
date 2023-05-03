// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
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

@Environment(JAVA_8)
@UseAgent("namespace_applicationinsights.json")
class MicrometerNamespaceTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /test");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/test");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
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
    assertThat(point.getMetricNamespace()).isEqualTo("testNamespace");
  }
}
