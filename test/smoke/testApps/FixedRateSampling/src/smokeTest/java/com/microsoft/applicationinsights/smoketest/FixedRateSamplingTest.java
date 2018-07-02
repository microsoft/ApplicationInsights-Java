package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import com.microsoft.applicationinsights.internal.schemav2.Envelope;

import org.junit.Test;

public class FixedRateSamplingTest extends AiSmokeTest {
    @Test
    @TargetUri("/fixedRateSampling")
    public void testFixedRateSamplingInExcludedTypes() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(100.0, getSampleRate("RequestData", 0), 0);
    }

    @Test
    @TargetUri("/fixedRateSampling")
    public void testFixedRateSamplingInIncludedTypes() {
        assertEquals(1, mockedIngestion.getCountForType("EventData"));
        assertEquals(50.0, getSampleRate("EventData", 0), 0);
    }

    @Test
    @TargetUri("/fixedRateSampling")
    public void testFixedRateSamplingNotInExcludedTypes() {
        assertEquals(1, mockedIngestion.getCountForType("MessageData"));
        assertEquals(100.0, getSampleRate("MessageData", 0), 0);
    }

    protected double getSampleRate(String type, int index) {
        Envelope envelope = mockedIngestion.getItemsByType(type).get(index);
        return envelope.getSampleRate();
    }

}