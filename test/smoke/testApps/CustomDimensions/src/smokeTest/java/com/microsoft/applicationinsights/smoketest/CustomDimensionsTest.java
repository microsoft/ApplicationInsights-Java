package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent("CustomDimensions")
public class CustomDimensionsTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        assertEquals("value", rd.getProperties().get("test"));
        assertEquals("/root", rd.getProperties().get("home"));

        assertTrue(rd.getSuccess());
    }
}
