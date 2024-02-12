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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class DetectUnexpectedOtelMetricsTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  private static final List<String> EXPECTED_METRIC_NAMES = new ArrayList<>();

  static {
    EXPECTED_METRIC_NAMES.add("_OTELRESOURCE_");
    EXPECTED_METRIC_NAMES.add("Loaded Class Count");
    EXPECTED_METRIC_NAMES.add("Current Thread Count");
    EXPECTED_METRIC_NAMES.add("\\Process(??APP_WIN32_PROC??)\\% Processor Time");
    EXPECTED_METRIC_NAMES.add("\\Process(??APP_WIN32_PROC??)\\% Processor Time Normalized");
    EXPECTED_METRIC_NAMES.add("\\Process(??APP_WIN32_PROC??)\\Private Bytes");
    EXPECTED_METRIC_NAMES.add("\\Memory\\Available Bytes");
    EXPECTED_METRIC_NAMES.add("\\Process(??APP_WIN32_PROC??)\\IO Data Bytes/sec");
    EXPECTED_METRIC_NAMES.add("\\Processor(_Total)\\% Processor Time");
    EXPECTED_METRIC_NAMES.add("Suspected Deadlocked Threads");
    EXPECTED_METRIC_NAMES.add("Heap Memory Used (MB)");
    EXPECTED_METRIC_NAMES.add("% Of Max Heap Memory Used");
    EXPECTED_METRIC_NAMES.add("GC Total Count");
    EXPECTED_METRIC_NAMES.add("GC Total Time");
  }

  @Test
  @TargetUri("/app")
  void testApp() throws Exception {
    // verify no unexpected otel metrics, expect an TimeoutException being thrown
    assertThatThrownBy(
            () ->
                testing.mockedIngestion.waitForItems(
                    "MetricData",
                    envelope -> {
                      MetricData md = (MetricData) ((Data<?>) envelope.getData()).getBaseData();
                      return !EXPECTED_METRIC_NAMES.contains(md.getMetrics().get(0).getName())
                          && !md.getProperties().containsKey("_MS.MetricId");
                    },
                    1))
        .isInstanceOf(TimeoutException.class);
  }

  // wait for at least one unexpected otel metrics for failure case or timeout for success
  public List<Envelope> waitForItemsUnexpectedOtelMetric(String type, Predicate<Envelope> condition)
      throws InterruptedException, ExecutionException, TimeoutException {
    return testing.mockedIngestion.waitForItems(
        new Predicate<Envelope>() {
          @Override
          public boolean test(Envelope input) {
            if (!input.getData().getBaseType().equals(type)) {
              return false;
            }
            MetricData md = (MetricData) ((Data<?>) input.getData()).getBaseData();
            if ("_OTELRESOURCE_".equals(md.getMetrics().get(0).getName())
                || md.getProperties().containsKey("_MS.MetricId")) {
              return false;
            }
            return condition.test(input);
          }
        },
        1,
        10,
        TimeUnit.SECONDS);
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends DetectUnexpectedOtelMetricsTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends DetectUnexpectedOtelMetricsTest {}
}
