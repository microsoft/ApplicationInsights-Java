// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights4.json")
abstract class SamplingOverrides4Test {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "/health-check", callCount = 100)
  void testSampling() throws Exception {
    // super super low chance that number of sampled requests is less than 25
    long start = System.nanoTime();
    while (testing.mockedIngestion.getCountForType("RequestData") < 25
        && NANOSECONDS.toSeconds(System.nanoTime() - start) < 10) {}
    // wait ten more seconds to before checking that we didn't receive too many
    Thread.sleep(SECONDS.toMillis(10));
    int requestCount = testing.mockedIngestion.getCountForType("RequestData");
    int dependencyCount = testing.mockedIngestion.getCountForType("RemoteDependencyData");
    int logCount = testing.mockedIngestion.getCountForType("MessageData");
    // super super low chance that number of sampled requests/dependencies/traces
    // is less than 25 or greater than 75
    assertThat(requestCount).isGreaterThanOrEqualTo(25);
    assertThat(requestCount).isLessThanOrEqualTo(75);
    // super super low chance that number of sampled dependencies/traces
    // is less than 2 or greater than 20
    assertThat(dependencyCount).isGreaterThanOrEqualTo(2);
    assertThat(dependencyCount).isLessThanOrEqualTo(20);
    assertThat(logCount).isGreaterThanOrEqualTo(2);
    assertThat(logCount).isLessThanOrEqualTo(20);

    testing
        .mockedIngestion
        .getItemsEnvelopeDataType("RequestData")
        .forEach(
            item -> {
              assertThat(item.getSampleRate()).isEqualTo(2);
            });
    testing
        .mockedIngestion
        .getItemsEnvelopeDataType("RemoteDependencyData")
        .forEach(
            item -> {
              assertThat(item.getSampleRate()).isEqualTo(10);
            });
    testing
        .mockedIngestion
        .getItemsEnvelopeDataType("MessageData")
        .forEach(
            item -> {
              assertThat(item.getSampleRate()).isEqualTo(10);
            });
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends SamplingOverrides4Test {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends SamplingOverrides4Test {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends SamplingOverrides4Test {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends SamplingOverrides4Test {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends SamplingOverrides4Test {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends SamplingOverrides4Test {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends SamplingOverrides4Test {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends SamplingOverrides4Test {}

  @Environment(TOMCAT_8_JAVA_23)
  static class Tomcat8Java23Test extends SamplingOverrides4Test {}

  @Environment(TOMCAT_8_JAVA_23_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends SamplingOverrides4Test {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends SamplingOverrides4Test {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends SamplingOverrides4Test {}
}
