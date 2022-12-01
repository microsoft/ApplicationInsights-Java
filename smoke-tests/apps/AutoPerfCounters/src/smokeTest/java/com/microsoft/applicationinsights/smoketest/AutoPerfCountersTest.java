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

import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class AutoPerfCountersTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "index.jsp")
  void testPerformanceCounterData() throws Exception {
    System.out.println("Waiting for performance data...");
    long start = System.currentTimeMillis();

    int timeout = 10;

    Envelope availableMem =
        testing.mockedIngestion.waitForItem(
            getPerfMetricPredicate("\\Memory\\Available Bytes"), timeout, TimeUnit.SECONDS);
    Envelope totalCpu =
        testing.mockedIngestion.waitForItem(
            getPerfMetricPredicate("\\Processor(_Total)\\% Processor Time"),
            timeout,
            TimeUnit.SECONDS);

    Envelope processIo =
        testing.mockedIngestion.waitForItem(
            getPerfMetricPredicate("\\Process(??APP_WIN32_PROC??)\\IO Data Bytes/sec"),
            timeout,
            TimeUnit.SECONDS);
    Envelope processMemUsed =
        testing.mockedIngestion.waitForItem(
            getPerfMetricPredicate("\\Process(??APP_WIN32_PROC??)\\Private Bytes"),
            timeout,
            TimeUnit.SECONDS);
    Envelope processCpu =
        testing.mockedIngestion.waitForItem(
            getPerfMetricPredicate("\\Process(??APP_WIN32_PROC??)\\% Processor Time"),
            timeout,
            TimeUnit.SECONDS);
    Envelope processCpuNormalized =
        testing.mockedIngestion.waitForItem(
            getPerfMetricPredicate("\\Process(??APP_WIN32_PROC??)\\% Processor Time Normalized"),
            timeout,
            TimeUnit.SECONDS);
    System.out.println("PerformanceCounterData are good: " + (System.currentTimeMillis() - start));

    MetricData metricMem = SmokeTestExtension.getBaseData(availableMem);
    assertPerfMetric(metricMem);
    assertThat(metricMem.getMetrics().get(0).getName()).isEqualTo("\\Memory\\Available Bytes");

    MetricData pdCpu = SmokeTestExtension.getBaseData(totalCpu);
    assertPerfMetric(pdCpu);
    assertThat(pdCpu.getMetrics().get(0).getName())
        .isEqualTo("\\Processor(_Total)\\% Processor Time");

    assertPerfMetric(SmokeTestExtension.getBaseData(processIo));
    assertPerfMetric(SmokeTestExtension.getBaseData(processMemUsed));
    assertPerfMetric(SmokeTestExtension.getBaseData(processCpu));
    assertPerfMetric(SmokeTestExtension.getBaseData(processCpuNormalized));

    start = System.currentTimeMillis();
    System.out.println("Waiting for metric data...");
    Envelope deadlocks =
        testing.mockedIngestion.waitForItem(
            getPerfMetricPredicate("Suspected Deadlocked Threads"), timeout, TimeUnit.SECONDS);
    Envelope heapUsed =
        testing.mockedIngestion.waitForItem(
            getPerfMetricPredicate("Heap Memory Used (MB)"), timeout, TimeUnit.SECONDS);
    Envelope gcTotalCount =
        testing.mockedIngestion.waitForItem(
            getPerfMetricPredicate("GC Total Count"), timeout, TimeUnit.SECONDS);
    Envelope gcTotalTime =
        testing.mockedIngestion.waitForItem(
            getPerfMetricPredicate("GC Total Time"), timeout, TimeUnit.SECONDS);
    System.out.println("MetricData are good: " + (System.currentTimeMillis() - start));

    MetricData mdDeadlocks = SmokeTestExtension.getBaseData(deadlocks);
    assertPerfMetric(mdDeadlocks);
    assertThat(mdDeadlocks.getMetrics().get(0).getValue()).isEqualTo(0);

    MetricData mdHeapUsed = SmokeTestExtension.getBaseData(heapUsed);
    assertPerfMetric(mdHeapUsed);
    assertThat(mdHeapUsed.getMetrics().get(0).getValue()).isGreaterThan(0);

    MetricData mdGcTotalCount = SmokeTestExtension.getBaseData(gcTotalCount);
    assertPerfMetric(mdGcTotalCount);

    MetricData mdGcTotalTime = SmokeTestExtension.getBaseData(gcTotalTime);
    assertPerfMetric(mdGcTotalTime);
  }

  private void assertPerfMetric(MetricData perfMetric) {
    List<DataPoint> metrics = perfMetric.getMetrics();
    assertThat(metrics).hasSize(1);
  }

  private static Predicate<Envelope> getPerfMetricPredicate(String name) {
    Objects.requireNonNull(name, "name");
    return new Predicate<Envelope>() {
      @Override
      public boolean test(Envelope input) {
        if (!input.getData().getBaseType().equals("MetricData")) {
          return false;
        }
        MetricData md = SmokeTestExtension.getBaseData(input);
        return name.equals(md.getMetrics().get(0).getName());
      }
    };
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends AutoPerfCountersTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends AutoPerfCountersTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends AutoPerfCountersTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends AutoPerfCountersTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends AutoPerfCountersTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends AutoPerfCountersTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends AutoPerfCountersTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends AutoPerfCountersTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends AutoPerfCountersTest {}
}
