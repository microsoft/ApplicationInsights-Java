package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.google.common.base.Stopwatch;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import org.junit.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@UseAgent("sampling")
public class SamplingTest extends AiSmokeTest {

    @Test
    @TargetUri(value = "/sampling", callCount = 100)
    public void testSampling() throws Exception {
        // super super low chance that number of sampled requests is less than 25
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (mockedIngestion.getCountForType("RequestData") < 25 && stopwatch.elapsed(SECONDS) < 10) {
        }
        // wait ten more seconds before checking that we didn't receive too many
        Thread.sleep(SECONDS.toMillis(10));
        List<Envelope> requestEnvelopes = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> dependencyEnvelopes = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");
        List<Envelope> eventEnvelopes = mockedIngestion.getItemsEnvelopeDataType("EventData");
        // super super low chance that number of sampled items is greater than 75
        assertThat(requestEnvelopes.size(), greaterThanOrEqualTo(25));
        assertThat(requestEnvelopes.size(), lessThanOrEqualTo(75));
        assertThat(dependencyEnvelopes.size(), greaterThanOrEqualTo(25));
        assertThat(dependencyEnvelopes.size(), lessThanOrEqualTo(75));
        assertThat(eventEnvelopes.size(), greaterThanOrEqualTo(25));
        assertThat(eventEnvelopes.size(), lessThanOrEqualTo(75));

        for (Envelope requestEnvelope : requestEnvelopes) {
            assertEquals(50, requestEnvelope.getSampleRate(), 0);
        }
        for (Envelope dependencyEnvelope : dependencyEnvelopes) {
            assertEquals(50, dependencyEnvelope.getSampleRate(), 0);
        }
        for (Envelope eventEnvelope : eventEnvelopes) {
            assertEquals(50, eventEnvelope.getSampleRate(), 0);
        }

        for (Envelope requestEnvelope : requestEnvelopes) {
            String operationId = requestEnvelope.getTags().get("ai.operation.id");
            mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
            mockedIngestion.waitForItemsInOperation("EventData", 1, operationId);
        }
    }
}
