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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class SamplingTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "/sampling", callCount = 100)
  void testSampling() throws Exception {
    // super super low chance that number of sampled requests is less than 25
    long start = System.nanoTime();
    while (testing.mockedIngestion.getCountForType("RequestData") < 25
        && NANOSECONDS.toSeconds(System.nanoTime() - start) < 10) {}
    // wait ten more seconds before checking that we didn't receive too many
    Thread.sleep(SECONDS.toMillis(10));

    List<Envelope> requestEnvelopes =
        testing.mockedIngestion.getItemsEnvelopeDataType("RequestData");
    List<Envelope> eventEnvelopes = testing.mockedIngestion.getItemsEnvelopeDataType("EventData");
    List<Envelope> messageEnvelopes =
        testing.mockedIngestion.getItemsEnvelopeDataType("MessageData");
    // super super low chance that number of sampled requests/dependencies/events
    // is less than 25 or greater than 75
    assertThat(requestEnvelopes.size()).isGreaterThanOrEqualTo(25);
    assertThat(requestEnvelopes.size()).isLessThanOrEqualTo(75);
    assertThat(eventEnvelopes.size()).isGreaterThanOrEqualTo(25);
    assertThat(eventEnvelopes.size()).isLessThanOrEqualTo(75);
    assertThat(messageEnvelopes.size()).isGreaterThanOrEqualTo(25);
    assertThat(messageEnvelopes.size()).isLessThanOrEqualTo(75);

    for (Envelope requestEnvelope : requestEnvelopes) {
      assertThat(requestEnvelope.getSampleRate()).isEqualTo(50);
    }
    for (Envelope eventEnvelope : eventEnvelopes) {
      assertThat(eventEnvelope.getSampleRate()).isEqualTo(50);
    }
    for (Envelope messageEnvelope : messageEnvelopes) {
      assertThat(messageEnvelope.getSampleRate()).isEqualTo(50);
    }

    for (Envelope requestEnvelope : requestEnvelopes) {
      String operationId = requestEnvelope.getTags().get("ai.operation.id");
      testing.mockedIngestion.waitForItemsInOperation("EventData", 1, operationId);
      testing.mockedIngestion.waitForItemsInOperation("MessageData", 1, operationId);
    }
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends SamplingTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends SamplingTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends SamplingTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends SamplingTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends SamplingTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends SamplingTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends SamplingTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends SamplingTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends SamplingTest {}
}
