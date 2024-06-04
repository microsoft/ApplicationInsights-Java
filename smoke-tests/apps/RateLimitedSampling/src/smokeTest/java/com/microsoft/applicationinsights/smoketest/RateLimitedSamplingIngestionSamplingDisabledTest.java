// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@UseAgent("applicationinsights-ingestion-sampling-disabled.json")
abstract class RateLimitedSamplingIngestionSamplingDisabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "/sampling", callCount = 1000)
  void testSampling() throws Exception {
    // give some time to collect
    Thread.sleep(SECONDS.toMillis(5));

    List<Envelope> rdEnvelopes = testing.mockedIngestion.getItemsEnvelopeDataType("RequestData");

    assertThat(rdEnvelopes.size()).isEqualTo(1000);

    for (Envelope rdEnvelope : rdEnvelopes) {
      assertThat(rdEnvelope.getSampleRate()).isEqualTo(99.99f);
    }
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends RateLimitedSamplingIngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends RateLimitedSamplingIngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends RateLimitedSamplingIngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends RateLimitedSamplingIngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends RateLimitedSamplingIngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends RateLimitedSamplingIngestionSamplingDisabledTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends RateLimitedSamplingIngestionSamplingDisabledTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends RateLimitedSamplingIngestionSamplingDisabledTest {}
}
