package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent
public class GrpcTest extends AiSmokeTest {

    @Test
    @TargetUri("/simple")
    public void doSimpleTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

        Envelope rdEnvelope1 = getRequestEnvelope(rdList, "simple");
        Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/SayHello");
        String operationId = rdEnvelope1.getTags().get("ai.operation.id");

        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
        // individual messages are captured as events (and exported as traces) on CLIENT/SERVER spans
        mockedIngestion.waitForItemsInOperation("EventData", 2, operationId);

        Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "HelloController.simple");
        Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "example.Greeter/SayHello");

        RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd2 = (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();

        // TODO this is not correct (or at least not ideal)
        //  see https://msazure.visualstudio.com/One/_workitems/edit/8687985
        assertEquals("grpc", rdd2.getTarget());

        assertTrue(rd1.getProperties().isEmpty());
        assertTrue(rd1.getSuccess());

        assertTrue(rdd1.getProperties().isEmpty());
        assertTrue(rdd1.getSuccess());

        assertTrue(rdd2.getProperties().isEmpty());
        assertTrue(rdd2.getSuccess());

        assertParentChild(rd1.getId(), rdEnvelope1, rddEnvelope1);
        assertParentChild(rdd1.getId(), rddEnvelope1, rddEnvelope2);
        assertParentChild(rdd2.getId(), rddEnvelope2, rdEnvelope2);
    }

    @Test
    @TargetUri("/conversation")
    public void doConversationTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

        Envelope rdEnvelope1 = getRequestEnvelope(rdList, "/conversation");
        Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/Conversation");
        String operationId = rdEnvelope1.getTags().get("ai.operation.id");

        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
        // individual messages are captured as events on CLIENT/SERVER spans
        mockedIngestion.waitForItemsInOperation("EventData", 3, operationId);

        Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "HelloController.conversation");
        Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "example.Greeter/Conversation");

        RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd2 = (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();

        assertEquals("grpc", rdd2.getTarget());

        assertTrue(rd1.getProperties().isEmpty());
        assertTrue(rd1.getSuccess());

        assertTrue(rdd1.getProperties().isEmpty());
        assertTrue(rdd1.getSuccess());

        assertTrue(rdd2.getProperties().isEmpty());
        assertTrue(rdd2.getSuccess());

        assertParentChild(rd1.getId(), rdEnvelope1, rddEnvelope1);
        assertParentChild(rdd1.getId(), rddEnvelope1, rddEnvelope2);
        assertParentChild(rdd2.getId(), rddEnvelope2, rdEnvelope2);
    }

    private static Envelope getRequestEnvelope(List<Envelope> envelopes, String name) {
        for (Envelope envelope : envelopes) {
            RequestData rd = (RequestData) ((Data<?>) envelope.getData()).getBaseData();
            if (rd.getName().equals(name)) {
                return envelope;
            }
        }
        throw new IllegalStateException("Could not find request with name: " + name);
    }

    private static Envelope getDependencyEnvelope(List<Envelope> envelopes, String name) {
        for (Envelope envelope : envelopes) {
            RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) envelope.getData()).getBaseData();
            if (rdd.getName().equals(name)) {
                return envelope;
            }
        }
        throw new IllegalStateException("Could not find dependency with name: " + name);
    }

    private static void assertParentChild(String parentId, Envelope parentEnvelope, Envelope childEnvelope) {
        String operationId = parentEnvelope.getTags().get("ai.operation.id");

        assertNotNull(operationId);

        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));
        assertEquals(parentId, childEnvelope.getTags().get("ai.operation.parentId"));
    }
}
