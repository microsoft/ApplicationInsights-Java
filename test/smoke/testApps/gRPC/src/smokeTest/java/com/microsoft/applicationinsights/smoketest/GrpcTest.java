package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;

@UseAgent
public class GrpcTest extends AiSmokeTest {

    @Test
    @TargetUri("/simple")
    public void doSimpleTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

        Envelope rdEnvelope1 = getRequestEnvelope(rdList, "example.Greeter/SayHello");
        String operationId = rdEnvelope1.getTags().get("ai.operation.id");

        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
        // individual messages are captured as events (and exported as traces) on CLIENT/SERVER spans
        mockedIngestion.waitForItemsInOperation("EventData", 2, operationId);

        Envelope rdEnvelope2 = getRequestEnvelope(rdList, "GET /simple");
        Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "example.Greeter/SayHello");
        Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "HelloController.simple");

        RequestData rd1 = (RequestData) ((Data) rdEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data) rddEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd2 = (RemoteDependencyData) ((Data) rddEnvelope2.getData()).getBaseData();

        assertParentChild(rdd2.getId(), rdEnvelope2, rddEnvelope1);
        assertParentChild(rdd1.getId(), rddEnvelope2, rdEnvelope1);
    }

    @Test
    @TargetUri("/conversation")
    public void doConversationTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

        Envelope rdEnvelope1 = getRequestEnvelope(rdList, "example.Greeter/Conversation");
        String operationId = rdEnvelope1.getTags().get("ai.operation.id");

        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
        // individual messages are captured as events on CLIENT/SERVER spans
        mockedIngestion.waitForItemsInOperation("EventData", 3, operationId);

        Envelope rdEnvelope2 = getRequestEnvelope(rdList, "GET /conversation");
        Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "example.Greeter/Conversation");
        Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "HelloController.conversation");

        RequestData rd1 = (RequestData) ((Data) rdEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data) rddEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd2 = (RemoteDependencyData) ((Data) rddEnvelope2.getData()).getBaseData();

        assertParentChild(rdd2.getId(), rdEnvelope2, rddEnvelope1);
        assertParentChild(rdd1.getId(), rddEnvelope2, rdEnvelope1);
    }

    private static Envelope getRequestEnvelope(List<Envelope> envelopes, String name) {
        for (Envelope envelope : envelopes) {
            RequestData rd = (RequestData) ((Data) envelope.getData()).getBaseData();
            if (rd.getName().equals(name)) {
                return envelope;
            }
        }
        throw new IllegalStateException("Could not find request with name: " + name);
    }

    private static Envelope getDependencyEnvelope(List<Envelope> envelopes, String name) {
        for (Envelope envelope : envelopes) {
            RemoteDependencyData rdd = (RemoteDependencyData) ((Data) envelope.getData()).getBaseData();
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
