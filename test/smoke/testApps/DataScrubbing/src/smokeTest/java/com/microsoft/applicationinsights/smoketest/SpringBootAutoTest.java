package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;
import static org.junit.Assert.*;

@UseAgent("spanProcessor")
public class SpringBootAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        assertNotNull(rd.getProperties().get("attribute1"));
        assertEquals("123", rd.getProperties().get("attribute1"));
        //assertEquals("redacted", rd.getProperties().get("http.client_ip"));
    }

    @Test
    @TargetUri("/sensitivedata")
    public void doSimpleTestPiiData() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        System.out.println(rd.getUrl());
      //  assertEquals("redacted", rd.getProperties().get("net.peer.ip"));
      //  assertEquals("redacted", rd.getProperties().get("net.peer.port"));
    }
}
