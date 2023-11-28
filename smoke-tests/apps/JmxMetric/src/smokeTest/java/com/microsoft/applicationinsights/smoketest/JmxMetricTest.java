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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.model.HttpRequest;
import io.opentelemetry.proto.metrics.v1.Metric;

@UseAgent
abstract class JmxMetricTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().useOtlpEndpoint().setSelfDiagnosticsLevel("TRACE").build();

  /* These are the jmx metric names we have configured to collect in the applicationInsights.json:
   NameWithDot: An edge case where a dot in the mbean is not a path separator in the attribute, specifically when using org.weakref.jmx package
   DefaultJmxMetricNameOverride: If a customer overrides the default ThreadCount metric with their own metric name, we should be collecting the metric with the name the customer specified
   WildcardJmxMetric: This covers the case of ? and * in the objectName, with multiple matching object names. The expected metric value here is the sum of all the CollectionCount attribute values for each matching object name.
        The matching objectNames are: G1 Young Generation,type=GarbageCollector and G1 Old Generation,type=GarbageCollector.
   Loaded_Class_Count: This covers the case of collecting a default jmx metric that the customer did not specify in applicationInsights.json. Also note that there are underscores
        instead of spaces, as we are emitting the metric via OpenTelemetry now. When the upstream fixes the related bug (https://github.com/open-telemetry/opentelemetry-specification/issues/3422#issuecomment-1678116597),
        then we can fix the code to not use underscores as a replacement.
   BooleanJmxMetric: This covers the case of a jmx metric attribute with a boolean value.
   DotInAttributeNameAsPathSeparator: This covers the case of an attribute having a dot in the name as a path separator.
   GCOld: This is the G1 Old Generation,type=GarbageCollector object name & CollectionCount attribute. Used to verify the value of WildcardJmxMetric.
   GCYoung: This is the G1 Young Generation,type=GarbageCollector object name & CollectionCount attribute. Used to verify the value of WildcardJmxMetric
   */
  static final Set<String> allowedMetrics = new HashSet<>(
      Arrays.asList("NameWithDot", "DefaultJmxMetricNameOverride", "WildcardJmxMetric", "Loaded_Class_Count", "BooleanJmxMetric", "DotInAttributeNameAsPathSeparator","GCOld", "GCYoung"));

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
        .atMost(10, SECONDS)
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

              // confirm that all metrics received once or twice in the span of 10s
              assertThat(occurrences.keySet()).hasSize(8);
              for(int value : occurrences.values()) {
                assertThat(value).isBetween(1,2);
              }

            });
  }

  private void verifyJmxMetricsSenttoBreeze() throws Exception {
    List<Envelope> metricItems =
        testing.mockedIngestion.waitForItems(
            envelope -> isJmxMetric(envelope), 1, 10, TimeUnit.SECONDS);

    assertThat(metricItems).hasSizeBetween(8,16);

    Set<String> metricNames = new HashSet<>();
    double wildcardValueSum = 0;
    double gcOldSum = 0;
    double gcYoungSum = 0;
    for (Envelope envelope : metricItems)
    {
      MetricData metricData = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
      List<DataPoint> points = metricData.getMetrics();
      assertThat(points).hasSize(1);
      String metricName = points.get(0).getName();
      metricNames.add(metricName);

      // verifying values of some metrics
      double value = points.get(0).getValue();
      if (metricName.equals("NameWithDot")) {
        assertThat(value).isEqualTo(5);
      } if (metricName.equals("GCOld")) {
        gcOldSum += value;
      } if (metricName.equals("GCYoung")) {
        gcYoungSum += value;
      } if (metricName.equals("WildcardJmxMetric")) {
        wildcardValueSum += value;
      } if (metricName.equals("BooleanJmxMetric")) {
        assertThat(value == 1.0 || value == 0.0);
      }
    }
    assertThat(wildcardValueSum == gcOldSum + gcYoungSum);
    assertThat(metricNames).containsAll(allowedMetrics);
  }

  private static boolean isJmxMetric(Envelope envelope) {
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
