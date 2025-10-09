// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_25_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class AutoPerfCountersTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "index.jsp")
  void testPerformanceCounterData() {

    testing.waitAndAssertMetric("\\Memory\\Available Bytes", MetricAssert::hasValueGreaterThanZero);

    testing.waitAndAssertMetric(
        "\\Processor(_Total)\\% Processor Time", MetricAssert::hasValueGreaterThanZero);

    testing.waitAndAssertMetric("\\Process(??APP_WIN32_PROC??)\\IO Data Bytes/sec", metric -> {});

    testing.waitAndAssertMetric(
        "\\Process(??APP_WIN32_PROC??)\\Private Bytes", MetricAssert::hasValueGreaterThanZero);

    testing.waitAndAssertMetric(
        "\\Process(??APP_WIN32_PROC??)\\% Processor Time", MetricAssert::hasValueGreaterThanZero);

    testing.waitAndAssertMetric(
        "\\Process(??APP_WIN32_PROC??)\\% Processor Time Normalized",
        MetricAssert::hasValueGreaterThanZero);

    testing.waitAndAssertMetric("Suspected Deadlocked Threads", metric -> metric.hasValue(0));

    testing.waitAndAssertMetric("Heap Memory Used (MB)", MetricAssert::hasValueGreaterThanZero);

    testing.waitAndAssertMetric("GC Total Count", metric -> {});
    testing.waitAndAssertMetric("GC Total Time", metric -> {});
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

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends AutoPerfCountersTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends AutoPerfCountersTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends AutoPerfCountersTest {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends AutoPerfCountersTest {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends AutoPerfCountersTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends AutoPerfCountersTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends AutoPerfCountersTest {}
}
