/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_18;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class SamplingTest {

  @RegisterExtension static final SmokeTestExtension testing = new SmokeTestExtension();

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
    // super super low chance that number of sampled requests/dependencies/events
    // is less than 25 or greater than 75
    assertThat(requestEnvelopes.size()).isGreaterThanOrEqualTo(25);
    assertThat(requestEnvelopes.size()).isLessThanOrEqualTo(75);
    assertThat(eventEnvelopes.size()).isGreaterThanOrEqualTo(25);
    assertThat(eventEnvelopes.size()).isLessThanOrEqualTo(75);

    for (Envelope requestEnvelope : requestEnvelopes) {
      assertThat(requestEnvelope.getSampleRate()).isEqualTo(50);
    }
    for (Envelope eventEnvelope : eventEnvelopes) {
      assertThat(eventEnvelope.getSampleRate()).isEqualTo(50);
    }

    for (Envelope requestEnvelope : requestEnvelopes) {
      String operationId = requestEnvelope.getTags().get("ai.operation.id");
      testing.mockedIngestion.waitForItemsInOperation("EventData", 1, operationId);
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

  @Environment(TOMCAT_8_JAVA_18)
  static class Tomcat8Java18Test extends SamplingTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends SamplingTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends SamplingTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends SamplingTest {}
}
