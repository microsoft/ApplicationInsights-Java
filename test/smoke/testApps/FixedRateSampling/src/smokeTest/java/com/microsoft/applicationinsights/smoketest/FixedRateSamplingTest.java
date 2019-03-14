package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import org.junit.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class FixedRateSamplingTest extends AiSmokeTest {
    @Test
    @TargetUri("/fixedRateSampling")
    public void testFixedRateSamplingInExcludedTypes() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(100.0, getSampleRate("RequestData", 0), Math.ulp(50.0));
    }

    @Test
    @TargetUri(value = "/fixedRateSampling", delay = 10000)
    public void testFixedRateSamplingInIncludedTypes() {
        int count = mockedIngestion.getCountForType("EventData");
        assertThat(count, both(greaterThanOrEqualTo(40)).and(lessThanOrEqualTo(60)));
        assertEquals(50.0, getSampleRate("EventData", 0), Math.ulp(50.0));
    }

    @Test
    @TargetUri("/fixedRateSampling")
    public void testFixedRateSamplingNotInExcludedTypes() {
        assertEquals(1, mockedIngestion.getCountForType("MessageData"));
        assertEquals(100.0, getSampleRate("MessageData", 0), Math.ulp(50.0));
    }

    protected double getSampleRate(String type, int index) {
        Envelope envelope = mockedIngestion.getItemsEnvelopeDataType(type).get(index);
        return envelope.getSampleRate();
    }

}