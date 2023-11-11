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
import static org.mockserver.model.HttpRequest.request;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.model.HttpRequest;
import io.opentelemetry.proto.metrics.v1.Metric;

@UseAgent
abstract class JmxMetricTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().useOtlpEndpoint().setSelfDiagnosticsLevel("TRACE").build();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    testing.getTelemetry(0);
    verifyJmxMetricsSenttoBreeze();
    verifyJmxMetricsSentToOtlpEndpoint();
  }
  @SuppressWarnings("PreferJavaTimeOverload")
  private void verifyJmxMetricsSentToOtlpEndpoint() {
    await()
        .atMost(60, SECONDS)
        .untilAsserted(
            () -> {
              HttpRequest[] requests =
                  testing
                      .mockedOtlpIngestion
                      .getCollectorServer()
                      .retrieveRecordedRequests(request());

              // verify metrics
              List<Metric> metrics =
                  testing.mockedOtlpIngestion.extractMetricsFromRequests(requests);
              assertThat(metrics)
                  .extracting(Metric::getName)
                  .contains("NameWithDot", "DemoThreadCount");
            });
  }

  private void verifyJmxMetricsSenttoBreeze() throws Exception {
    List<Envelope> metricItems =
        testing.mockedIngestion.waitForItems(
            envelope -> isJmxMetric(envelope), 1, 10, TimeUnit.SECONDS);

    assertThat(metricItems).hasSize(2);

    Set<String> metricNames = new HashSet<>();
    for (Envelope envelope : metricItems)
    {
      MetricData metricData = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      List<DataPoint> points = metricData.getMetrics();
      assertThat(points).hasSize(1);
      String metricName = points.get(0).getName();
      if (metricName.equals("NameWithDot")) {
        assertThat(points.get(0).getValue()).isEqualTo(5);
      }
      metricNames.add(metricName);
    }
    assertThat(metricNames).contains("NameWithDot", "DemoThreadCount");
  }

  private static boolean isJmxMetric(Envelope envelope) {
    if (!envelope.getData().getBaseType().equals("MetricData")) {
      return false;
    }
    MetricData md = SmokeTestExtension.getBaseData(envelope);
    String incomingMetricName = md.getMetrics().get(0).getName();
    return incomingMetricName.equals("NameWithDot") || incomingMetricName.equals("DemoThreadCount");
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends JmxMetricTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends JmxMetricTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends JmxMetricTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends JmxMetricTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends JmxMetricTest {}
}
