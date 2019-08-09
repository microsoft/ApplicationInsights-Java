package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.*;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

public class WebAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() {
        assertTrue("mocked ingestion has no data", mockedIngestion.hasData());
        assertTrue("mocked ingestion has 0 items", mockedIngestion.getItemCount() > 0);

        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        validateSdkName(d, "java-web-auto");
    }

    private void validateSdkName(Domain data, String sdkName) {
        Envelope envelope = mockedIngestion.getEnvelopeForBaseData(data);
        String sdkVersion = envelope.getTags().get("ai.internal.sdkVersion");
        assertThat(sdkVersion, startsWith(sdkName + ":"));
    }
}
