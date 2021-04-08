package com.microsoft.applicationinsights.smoketestapp;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.DependencyContainer;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import com.microsoft.applicationinsights.smoketest.WithDependencyContainers;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent("disabled_mongo")
@WithDependencyContainers(
        @DependencyContainer(
                value = "mongo:4",
                portMapping = "27017",
                hostnameEnvironmentVariable = "MONGO")
)
public class MongoDisabledTest extends AiSmokeTest {

    @Test
    @TargetUri("/mongo")
    public void mongo() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertEquals("GET /MongoDB/*", rd.getName());
        assertEquals("200", rd.getResponseCode());
        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        // sleep a bit and make sure no mongo dependencies are reported
        Thread.sleep(5000);
        assertEquals(0, mockedIngestion.getCountForType("RemoteDependencyData"));
    }
}
