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

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import java.util.List;
import org.junit.Test;

@UseAgent("sampling")
public class SamplingTest extends AiSmokeTest {

  @Test
  @TargetUri(value = "/sampling", callCount = 100)
  public void testSampling() throws Exception {
    // super super low chance that number of sampled requests is less than 25
    long start = System.nanoTime();
    while (mockedIngestion.getCountForType("RequestData") < 25
        && NANOSECONDS.toSeconds(System.nanoTime() - start) < 10) {}
    // wait ten more seconds before checking that we didn't receive too many
    Thread.sleep(SECONDS.toMillis(10));

    List<Envelope> requestEnvelopes = mockedIngestion.getItemsEnvelopeDataType("RequestData");
    List<Envelope> eventEnvelopes = mockedIngestion.getItemsEnvelopeDataType("EventData");
    // super super low chance that number of sampled requests/dependencies/events
    // is less than 25 or greater than 75
    assertThat(requestEnvelopes.size(), greaterThanOrEqualTo(25));
    assertThat(requestEnvelopes.size(), lessThanOrEqualTo(75));
    assertThat(eventEnvelopes.size(), greaterThanOrEqualTo(25));
    assertThat(eventEnvelopes.size(), lessThanOrEqualTo(75));

    for (Envelope requestEnvelope : requestEnvelopes) {
      assertEquals(50, requestEnvelope.getSampleRate(), 0);
    }
    for (Envelope eventEnvelope : eventEnvelopes) {
      assertEquals(50, eventEnvelope.getSampleRate(), 0);
    }

    for (Envelope requestEnvelope : requestEnvelopes) {
      String operationId = requestEnvelope.getTags().get("ai.operation.id");
      mockedIngestion.waitForItemsInOperation("EventData", 1, operationId);
    }
  }
}
