package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent
@WithDependencyContainers({
        @DependencyContainer(
                value = "confluentinc/cp-zookeeper",
                portMapping = "2181",
                environmentVariables = {
                        "ZOOKEEPER_CLIENT_PORT=2181"
                },
                hostnameEnvironmentVariable = "ZOOKEEPER"),
        @DependencyContainer(
                value = "confluentinc/cp-kafka",
                portMapping = "9092",
                environmentVariables = {
                        "KAFKA_ZOOKEEPER_CONNECT=${ZOOKEEPER}:2181",
                        "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://${CONTAINERNAME}:9092",
                        "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1"
                },
                hostnameEnvironmentVariable = "KAFKA")
})
public class SpringCloudStreamTest extends AiSmokeTest {

    @Test
    @TargetUri("/sendMessage")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

        Envelope rdEnvelope1 = rdList.get(0);
        String operationId = rdEnvelope1.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rdEnvelope2 = rdList.get(1);
        Envelope rddEnvelope1 = rddList.get(0);
        Envelope rddEnvelope2 = rddList.get(1);

        RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
        RequestData rd2 = (RequestData) ((Data<?>) rdEnvelope2.getData()).getBaseData();
        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd2 = (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();

        if (!rdd1.getName().equals("GreetingsController.sendMessage")) {
            RemoteDependencyData rddTemp = rdd1;
            rdd1 = rdd2;
            rdd2 = rddTemp;

            Envelope rddEnvelopeTemp = rddEnvelope1;
            rddEnvelope1 = rddEnvelope2;
            rddEnvelope2 = rddEnvelopeTemp;
        }

        assertEquals("GET /sendMessage", rd1.getName());
        assertTrue(rd1.getProperties().isEmpty());
        assertTrue(rd1.getSuccess());

        assertEquals("GreetingsController.sendMessage", rdd1.getName());
        assertTrue(rdd1.getProperties().isEmpty());
        assertTrue(rdd1.getSuccess());

        assertEquals("greetings send", rdd2.getName());
        assertEquals("Queue Message | kafka", rdd2.getType());
        assertEquals("greetings", rdd2.getTarget());
        assertTrue(rdd2.getProperties().isEmpty());
        assertTrue(rdd2.getSuccess());

        assertEquals("greetings process", rd2.getName());
        assertEquals("greetings", rd2.getSource());
        assertTrue(rd2.getProperties().isEmpty());
        assertTrue(rd2.getSuccess());

        assertParentChild(rd1.getId(), rdEnvelope1, rddEnvelope1);
        assertParentChild(rdd1.getId(), rddEnvelope1, rddEnvelope2);
        assertParentChild(rdd2.getId(), rddEnvelope2, rdEnvelope2);
    }

    private static void assertParentChild(String parentId, Envelope parentEnvelope, Envelope childEnvelope) {
        String operationId = parentEnvelope.getTags().get("ai.operation.id");

        assertNotNull(operationId);

        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));
        assertEquals(parentId, childEnvelope.getTags().get("ai.operation.parentId"));
    }
}
