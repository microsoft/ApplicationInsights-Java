package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent
public class SpringBootAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        // TODO verify browser and other envelope tags somewhere else
        assertTrue(rdEnvelope.getTags().get("ai.user.userAgent").startsWith("Apache-HttpClient/"));
        assertNotNull(rdEnvelope.getTags().get("ai.location.ip"));

        assertEquals("GET /SpringBootAuto/test", rd.getName());
        assertEquals("200", rd.getResponseCode());
        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());
    }
}
