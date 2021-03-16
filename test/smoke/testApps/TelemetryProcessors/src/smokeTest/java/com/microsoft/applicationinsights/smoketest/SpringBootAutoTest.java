package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;
import static org.junit.Assert.*;

@UseAgent("telemetryprocessors")
public class SpringBootAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        assertNotNull(rd.getProperties().get("attribute1"));
        assertEquals("testValue1", rd.getProperties().get("attribute1"));
        assertEquals("testValue2", rd.getProperties().get("attribute2"));
        assertNotNull(rd.getProperties().get("httpPath"));
        assertEquals("/TelemetryProcessors/test",rd.getProperties().get("httpPath"));
    }

    @Test
    @TargetUri("/sensitivedata")
    public void doSimpleTestPiiData() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        assertEquals("testValue1::testValue2", rd.getName());
        assertEquals("redacted", rd.getProperties().get("sensitiveAttribute1"));
    }

}
