// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class DotInJmxMetricTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().useOtlpEndpoint().setSelfDiagnosticsLevel("TRACE").build();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    testing.getTelemetry(0);

    List<Envelope> metricItems =
        testing.mockedIngestion.waitForItems(
            envelope -> isMetricWithName(envelope, "NameWithDot"), 1, 10, TimeUnit.SECONDS);

    MetricData data = (MetricData) ((Data<?>) metricItems.get(0).getData()).getBaseData();
    List<DataPoint> points = data.getMetrics();
    assertThat(points).hasSize(1);

    DataPoint point = points.get(0);
    assertThat(point.getValue()).isEqualTo(5);

    testing.mockedOtlpIngestion.verify("NameWithDot");

  }

  private static boolean isMetricWithName(Envelope envelope, String metricName) {
    if (!envelope.getData().getBaseType().equals("MetricData")) {
      return false;
    }
    MetricData md = SmokeTestExtension.getBaseData(envelope);
    return metricName.equals(md.getMetrics().get(0).getName());
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends DotInJmxMetricTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends DotInJmxMetricTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends DotInJmxMetricTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends DotInJmxMetricTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends DotInJmxMetricTest {}
}
