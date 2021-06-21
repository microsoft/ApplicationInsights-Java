package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import org.junit.*;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@UseAgent("sampling")
public class SamplingTest extends AiSmokeTest {

    @Test
    @TargetUri(value = "/sampling", callCount = 100)
    public void testSampling() throws Exception {
        // super super low chance that number of sampled requests is less than 25
        long start = System.nanoTime();
        while (mockedIngestion.getCountForType("RequestData") < 25
                && NANOSECONDS.toSeconds(System.nanoTime() - start) < 10) {
        }
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
