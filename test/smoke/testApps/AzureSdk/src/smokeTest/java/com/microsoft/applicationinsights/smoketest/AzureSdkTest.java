package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent("azuresdk")
public class AzureSdkTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void test() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope1 = rddList.get(0);
        Envelope rddEnvelope2 = rddList.get(1);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd2 = (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();

        if (!rdd1.getName().equals("TestController.test")) {
            RemoteDependencyData rddTemp = rdd1;
            rdd1 = rdd2;
            rdd2 = rddTemp;

            Envelope rddEnvelopeTemp = rddEnvelope1;
            rddEnvelope1 = rddEnvelope2;
            rddEnvelope2 = rddEnvelopeTemp;
        }

        assertEquals("/AzureSdk/test", rd.getName());
        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("TestController.test", rdd1.getName());
        assertTrue(rdd1.getProperties().isEmpty());
        assertTrue(rdd1.getSuccess());

        assertEquals("hello", rdd2.getName());
        assertTrue(rdd2.getProperties().isEmpty());
        assertTrue(rdd2.getSuccess());

        assertParentChild(rd.getId(), rdEnvelope, rddEnvelope1);
        assertParentChild(rdd1.getId(), rddEnvelope1, rddEnvelope2);
    }

    private static void assertParentChild(String parentId, Envelope parentEnvelope, Envelope childEnvelope) {
        String operationId = parentEnvelope.getTags().get("ai.operation.id");

        assertNotNull(operationId);

        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));
        assertEquals(parentId, childEnvelope.getTags().get("ai.operation.parentId"));
    }
}
