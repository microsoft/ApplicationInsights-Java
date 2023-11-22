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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Condition;
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

              Map<String, Integer> occurrences = new HashMap<>();
              Set<String> allowedMetrics = new HashSet<>(
                  Arrays.asList("NameWithDot", "DemoThreadCount", "DemoCurrentThreadCpuTime", "Loaded_Class_Count"));

              // counting all occurrences of each jmx metric
              for (Metric metric : metrics) {
                String metricName = metric.getName();
                if (allowedMetrics.contains(metricName))
                {
                  if (occurrences.containsKey(metricName)) {
                    occurrences.put(metricName, occurrences.get(metricName) + 1);
                  } else {
                    occurrences.put(metricName, 1);
                  }
                }
              }

              // confirm that all metrics recieved once or twice (depending on timing)
              assertThat(occurrences.keySet()).hasSize(4);
              for(int value : occurrences.values()) {
                assertThat(value).isBetween(1,2);
              }

              /*assertThat(metrics)
                  .extracting(Metric::getName)
                  .contains("NameWithDot", "DemoThreadCount","DemoCurrentThreadCpuTime", "Loaded_Class_Count");*/
            });
  }

  private void verifyJmxMetricsSenttoBreeze() throws Exception {
    List<Envelope> metricItems =
        testing.mockedIngestion.waitForItems(
            envelope -> isJmxMetric(envelope), 1, 10, TimeUnit.SECONDS);

    assertThat(metricItems).hasSizeBetween(4,8);

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
    assertThat(metricNames).contains("NameWithDot", "DemoThreadCount", "DemoCurrentThreadCpuTime", "Loaded_Class_Count");
  }

  private static boolean isJmxMetric(Envelope envelope) {
    Set<String> allowedMetrics = new HashSet<>(
        Arrays.asList("NameWithDot", "DemoThreadCount", "DemoCurrentThreadCpuTime", "Loaded_Class_Count"));
    if (!envelope.getData().getBaseType().equals("MetricData")) {
      return false;
    }
    MetricData md = SmokeTestExtension.getBaseData(envelope);
    String incomingMetricName = md.getMetrics().get(0).getName();
    return allowedMetrics.contains(incomingMetricName);
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
