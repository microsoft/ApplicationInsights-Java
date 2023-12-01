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
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-no-sampling.json")
abstract class NoSamplingTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "/no-sampling", callCount = 1000)
  void testNoSampling() throws Exception {
    long start = System.nanoTime();
    while (testing.mockedIngestion.getCountForType("RequestData") < 1000
        && NANOSECONDS.toSeconds(System.nanoTime() - start) < 10) {
      // just wait and do nothing
    }
    assertThat(testing.mockedIngestion.getCountForType("RequestData")).isEqualTo(1000);

    List<Envelope> requestEnvelopes =
        testing.mockedIngestion.getItemsEnvelopeDataType("RequestData");
    assertThat(requestEnvelopes.size()).isEqualTo(1000);
    List<Envelope> eventEnvelopes = testing.mockedIngestion.getItemsEnvelopeDataType("EventData");
    assertThat(eventEnvelopes.size()).isEqualTo(1000);
    List<Envelope> messageEnvelopes =
        testing.mockedIngestion.getItemsEnvelopeDataType("MessageData");
    assertThat(messageEnvelopes.size()).isEqualTo(1000);

    for (Envelope requestEnvelope : requestEnvelopes) {
      assertThat(requestEnvelope.getSampleRate()).isNull();
    }
    for (Envelope eventEnvelope : eventEnvelopes) {
      assertThat(eventEnvelope.getSampleRate()).isNull();
    }
    for (Envelope messageEnvelope : messageEnvelopes) {
      assertThat(messageEnvelope.getSampleRate()).isNull();
    }
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends NoSamplingTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends NoSamplingTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends NoSamplingTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends NoSamplingTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends NoSamplingTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends NoSamplingTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends NoSamplingTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends NoSamplingTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends NoSamplingTest {}
}
