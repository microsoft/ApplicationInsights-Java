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
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights3.json")
abstract class SamplingOverrides3Test {

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
    // is less than 2 or greater than 20
    assertThat(requestCount).isGreaterThanOrEqualTo(2);
    assertThat(requestCount).isLessThanOrEqualTo(20);
    assertThat(dependencyCount).isGreaterThanOrEqualTo(2);
    assertThat(dependencyCount).isLessThanOrEqualTo(20);
    assertThat(logCount).isGreaterThanOrEqualTo(2);
    assertThat(logCount).isLessThanOrEqualTo(20);

    testing
        .mockedIngestion
        .getItemsEnvelopeDataType("RequestData")
        .forEach(item -> assertThat(item.getSampleRate()).isEqualTo(10));
    testing
        .mockedIngestion
        .getItemsEnvelopeDataType("RemoteDependencyData")
        // even though the configured dependency sampling is 50%
        // since that is less than 100%, it should not exceed its parent request sampling percentage
        .forEach(item -> assertThat(item.getSampleRate()).isEqualTo(10));
    testing
        .mockedIngestion
        .getItemsEnvelopeDataType("MessageData")
        // even though the configured log message sampling is 50%
        // since that is less than 100%, it should not exceed its parent request sampling percentage
        .forEach(item -> assertThat(item.getSampleRate()).isEqualTo(10));
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends SamplingOverrides3Test {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends SamplingOverrides3Test {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends SamplingOverrides3Test {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends SamplingOverrides3Test {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends SamplingOverrides3Test {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends SamplingOverrides3Test {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends SamplingOverrides3Test {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends SamplingOverrides3Test {}

  @Environment(TOMCAT_8_JAVA_25)
  static class Tomcat8Java23Test extends SamplingOverrides3Test {}

  @Environment(TOMCAT_8_JAVA_25_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends SamplingOverrides3Test {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends SamplingOverrides3Test {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends SamplingOverrides3Test {}
}
