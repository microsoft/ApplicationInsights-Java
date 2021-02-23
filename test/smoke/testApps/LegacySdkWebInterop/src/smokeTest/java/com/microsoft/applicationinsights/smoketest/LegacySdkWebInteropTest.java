package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent
public class LegacySdkWebInteropTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        assertEquals("myvalue1", rd.getProperties().get("myattr1"));
        assertEquals("myvalue2", rd.getProperties().get("myattr2"));
        assertEquals("myuser", rdEnvelope.getTags().get("ai.user.id"));
        assertEquals("myspanname", rd.getName());
        assertEquals("mysource", rd.getSource());

        assertTrue(rd.getSuccess());
    }
}
