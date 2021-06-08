package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseAgent
public class SpringBootAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/spawn-another-java-process")
    public void spawnAnotherJavaProcess() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 2);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        if (!rdd.getName().equals("HTTP GET")) {
            rddEnvelope = rddList.get(0);
            rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();
        }

        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("HTTP GET", rdd.getName());
        assertEquals("Http", rdd.getType());
        assertEquals("www.bing.com", rdd.getTarget());
        assertEquals("https://www.bing.com/search?q=test", rdd.getData());
        assertTrue(rdd.getProperties().isEmpty());
        assertTrue(rdd.getSuccess());
    }
}
