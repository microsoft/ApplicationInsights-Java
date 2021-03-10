package com.microsoft.applicationinsights.smoketestapp;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent("disabled_jdbc")
public class JdbcDisabledTest extends AiSmokeTest {

    @Test
    @TargetUri("/hsqldbPreparedStatement")
    public void hsqldbPreparedStatement() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("/Jdbc/*", rd.getName());
        assertEquals("200", rd.getResponseCode());

        // sleep a bit and make sure no jdbc dependencies are reported
        Thread.sleep(5000);
        assertEquals(0, mockedIngestion.getCountForType("RemoteDependencyData"));
    }
}
