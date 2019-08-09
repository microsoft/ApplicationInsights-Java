package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class FixedRateSamplingTest extends AiSmokeTest {
    @Test
    @TargetUri("/fixedRateSampling")
    public void testFixedRateSamplingInExcludedTypes() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(100.0, getSampleRate("RequestData", 0), Math.ulp(50.0));
        RequestData rd = getTelemetryDataForType(0, "RequestData");
        validateSdkName(rd, "java-web-manual");
    }

    @Test
    @TargetUri(value = "/fixedRateSampling", delay = 10000)
    public void testFixedRateSamplingInIncludedTypes() {
        int count = mockedIngestion.getCountForType("EventData");
        assertThat(count, both(greaterThanOrEqualTo(40)).and(lessThanOrEqualTo(60)));
        assertEquals(50.0, getSampleRate("EventData", 0), Math.ulp(50.0));
        EventData ed = getTelemetryDataForType(0, "EventData");
        validateSdkName(ed, "java");
    }

    @Test
    @TargetUri("/fixedRateSampling")
    public void testFixedRateSamplingNotInExcludedTypes() {
        assertEquals(1, mockedIngestion.getCountForType("MessageData"));
        assertEquals(100.0, getSampleRate("MessageData", 0), Math.ulp(50.0));
        MessageData md = getTelemetryDataForType(0, "MessageData");
        validateSdkName(md, "java");
    }

    protected double getSampleRate(String type, int index) {
        Envelope envelope = mockedIngestion.getItemsEnvelopeDataType(type).get(index);
        return envelope.getSampleRate();
    }

    private void validateSdkName(Domain data, String sdkName) {
        Envelope envelope = mockedIngestion.getEnvelopeForBaseData(data);
        String sdkVersion = envelope.getTags().get("ai.internal.sdkVersion");
        assertThat(sdkVersion, startsWith(sdkName + ":"));
    }
}