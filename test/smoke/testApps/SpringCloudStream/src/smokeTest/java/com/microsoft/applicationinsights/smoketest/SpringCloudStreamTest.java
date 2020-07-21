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
        List<Envelope> rddList = mockedIngestion.waitForItemsInRequest("RemoteDependencyData", 1);

        Envelope rdEnvelope1 = rdList.get(0);
        Envelope rdEnvelope2 = rdList.get(1);
        Envelope rddEnvelope = rddList.get(0);

        RequestData rd1 = (RequestData) ((Data) rdEnvelope1.getData()).getBaseData();
        RequestData rd2 = (RequestData) ((Data) rdEnvelope2.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        if (!rd1.getName().equals("GET /sendMessage")) {
            // swap request and envelope 1 and 2
            Envelope tmpEnvelope = rdEnvelope1;
            rdEnvelope1 = rdEnvelope2;
            rdEnvelope2 = tmpEnvelope;
            RequestData tmp = rd1;
            rd1 = rd2;
            rd2 = tmp;
        }

        assertEquals("GET /sendMessage", rd1.getName());
        assertEquals("Kafka", rdd.getType());
        assertEquals("greetings", rdd.getName());
        assertEquals("greetings", rd2.getName());

        assertParentChild(rd1.getId(), rdEnvelope1, rddEnvelope);
        assertParentChild(rdd.getId(), rddEnvelope, rdEnvelope2);
    }

    private static void assertParentChild(String parentId, Envelope parentEnvelope, Envelope childEnvelope) {
        String operationId = parentEnvelope.getTags().get("ai.operation.id");

        assertNotNull(operationId);

        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));
        assertEquals(parentId, childEnvelope.getTags().get("ai.operation.parentId"));
    }
}
