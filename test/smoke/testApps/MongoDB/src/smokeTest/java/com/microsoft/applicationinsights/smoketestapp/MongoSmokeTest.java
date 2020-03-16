package com.microsoft.applicationinsights.smoketestapp;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.DependencyContainer;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import com.microsoft.applicationinsights.smoketest.WithDependencyContainers;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent
@WithDependencyContainers(
        @DependencyContainer(
                value = "mongo:4",
                portMapping = "27017",
                hostnameEnvironmentVariable = "MONGO")
)
public class MongoSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri("/mongo")
    public void mongo() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItemsInRequest("RemoteDependencyData", 1);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("mongo", rdd.getType());
        assertEquals("{\"find\": \"test\", \"$db\": \"?\"}", rdd.getName());
        assertTrue(rdd.getSuccess());

        assertParentChild(rd, rdEnvelope, rddEnvelope);
    }

    private static void assertParentChild(RequestData rd, Envelope rdEnvelope, Envelope childEnvelope) {
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        assertNotNull(operationId);
        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));

        String operationParentId = rdEnvelope.getTags().get("ai.operation.parentId");
        assertNull(operationParentId);

        assertEquals(rd.getId(), childEnvelope.getTags().get("ai.operation.parentId"));
    }
}
