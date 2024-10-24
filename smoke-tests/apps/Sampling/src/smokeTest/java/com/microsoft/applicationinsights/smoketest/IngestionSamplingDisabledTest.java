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

import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent("applicationinsights-ingestion-sampling-disabled.json")
abstract class IngestionSamplingDisabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri(value = "/simple", callCount = 100)
  void testNoIngestionSampling() throws Exception {
    long start = System.nanoTime();
    while (testing.mockedIngestion.getCountForType("RequestData") < 100
        && NANOSECONDS.toSeconds(System.nanoTime() - start) < 10) {
      // just wait and do nothing
    }
    Thread.sleep(SECONDS.toMillis(10));

    assertThat(testing.mockedIngestion.getCountForType("RequestData")).isEqualTo(100);

    List<Envelope> requestEnvelopes =
        testing.mockedIngestion.getItemsEnvelopeDataType("RequestData");
    assertThat(requestEnvelopes.size()).isEqualTo(100);
    List<Envelope> eventEnvelopes = testing.mockedIngestion.getItemsEnvelopeDataType("EventData");
    assertThat(eventEnvelopes.size()).isEqualTo(100);
    List<Envelope> messageEnvelopes =
        testing.mockedIngestion.getItemsEnvelopeDataType("MessageData");
    assertThat(messageEnvelopes.size()).isEqualTo(100);

    for (Envelope requestEnvelope : requestEnvelopes) {
      // 99.99 will suppress ingestion sampling while still resulting in item count 1
      assertThat(requestEnvelope.getSampleRate()).isEqualTo(99.99f);
    }
    for (Envelope eventEnvelope : eventEnvelopes) {
      // 99.99 will suppress ingestion sampling while still resulting in item count 1
      assertThat(eventEnvelope.getSampleRate()).isEqualTo(99.99f);
    }
    for (Envelope messageEnvelope : messageEnvelopes) {
      // 99.99 will suppress ingestion sampling while still resulting in item count 1
      assertThat(messageEnvelope.getSampleRate()).isEqualTo(99.99f);
    }
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends IngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends IngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends IngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends IngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends IngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends IngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends IngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends IngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_23)
  static class Tomcat8Java23Test extends IngestionSamplingDisabledTest {}

  @Environment(TOMCAT_8_JAVA_23_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends IngestionSamplingDisabledTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends IngestionSamplingDisabledTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends IngestionSamplingDisabledTest {}
}
