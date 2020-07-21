package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasName;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;

@UseAgent
public class GrpcTest extends AiSmokeTest {

    @Test
    @TargetUri("/simple")
    public void doSimpleTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);
        List<Envelope> rddList = mockedIngestion.waitForItemsInRequest("RemoteDependencyData", 1);
        // individual messages are captured as events (and exported as traces) on CLIENT/SERVER spans
        mockedIngestion.waitForItemsInRequest("EventData", 2);

        Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /simple");
        Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/SayHello");
        Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "example.Greeter/SayHello");

        RequestData rd1 = (RequestData) ((Data) rdEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data) rddEnvelope1.getData()).getBaseData();

        assertParentChild(rd1.getId(), rdEnvelope1, rddEnvelope1);
        assertParentChild(rdd1.getId(), rddEnvelope1, rdEnvelope2);
    }

    @Test
    @TargetUri("/conversation")
    public void doConversationTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);
        List<Envelope> rddList = mockedIngestion.waitForItemsInRequest("RemoteDependencyData", 1);
        // individual messages are captured as events on CLIENT/SERVER spans
        mockedIngestion.waitForItemsInRequest("EventData", 3);

        Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /conversation");
        Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/Conversation");
        Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "example.Greeter/Conversation");

        RequestData rd1 = (RequestData) ((Data) rdEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data) rddEnvelope1.getData()).getBaseData();

        assertParentChild(rd1.getId(), rdEnvelope1, rddEnvelope1);
        assertParentChild(rdd1.getId(), rddEnvelope1, rdEnvelope2);
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
