package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import org.junit.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class FixedRateSamplingTest extends AiSmokeTest {
    @Test
    @TargetUri("/fixedRateSampling")
    public void testFixedRateSamplingInExcludedTypes() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rd = rdList.get(0);
        assertEquals(100.0, rd.getSampleRate(), Math.ulp(50.0));
    }

    @Test
    @TargetUri(value = "/fixedRateSampling", callCount = 100)
    public void testFixedRateSamplingInIncludedTypes() throws Exception {
        mockedIngestion.waitForItems("RequestData", 100);
        List<Envelope> edList = mockedIngestion.getItemsEnvelopeDataType("EventData");
        // super super low chance that number of events sampled is less than 10 or greater than 90
        assertThat(edList.size(), both(greaterThanOrEqualTo(10)).and(lessThanOrEqualTo(90)));
        Envelope ed = edList.get(0);
        assertEquals(50.0, ed.getSampleRate(), Math.ulp(50.0));
    }

    @Test
    @TargetUri("/fixedRateSampling")
    public void testFixedRateSamplingNotInExcludedTypes() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> mdList = mockedIngestion.waitForItems("MessageData", 1);
        Envelope md = mdList.get(0);
        assertEquals(100.0, md.getSampleRate(), Math.ulp(50.0));
    }
}