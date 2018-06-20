package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import com.microsoft.applicationinsights.internal.schemav2.Envelope;

import org.junit.Test;

public class FixedRateSamplingTest extends AiSmokeTest {
    @Test
    @TargetUri("/fixedRateSampling")
    public void testFixedRateSampling() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("EventData"));

        assertEquals(50.0, getSampleRae("EventData", 0), 0);
        assertEquals(100.0, getSampleRae("RequestData", 0), 0);
    }

    protected double getSampleRae(String type, int index) {
        Envelope envelope = mockedIngestion.getItemsByType(type).get(index);
        return envelope.getSampleRate();
    }

}