package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent("disabled_kafka")
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
public class KafkaDisabledTest extends AiSmokeTest {

    @Test
    @TargetUri("/sendMessage")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        Envelope rddEnvelope = rddList.get(0);
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("/sendMessage", rd.getName());
        assertEquals("200", rd.getResponseCode());

        assertEquals("HelloController.sendMessage", rdd.getName());

        assertParentChild(rd.getId(), rdEnvelope, rddEnvelope);

        // verify the downstream http dependency that is no longer part of the same trace
        rddList = mockedIngestion.waitForItems("RemoteDependencyData", 2);
        rddEnvelope = rddList.get(0);
        if (operationId.equals(rddEnvelope.getTags().get("ai.operation.id"))) {
            rddEnvelope = rddList.get(1);
        }
        rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        assertEquals("HTTP GET", rdd.getName());
        assertEquals("https://www.bing.com", rdd.getData());

        // sleep a bit and make sure no kafka "requests" or dependencies are reported
        Thread.sleep(5000);
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(2, mockedIngestion.getCountForType("RemoteDependencyData"));
    }

    private static void assertParentChild(String parentId, Envelope parentEnvelope, Envelope childEnvelope) {
        String operationId = parentEnvelope.getTags().get("ai.operation.id");

        assertNotNull(operationId);

        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));
        assertEquals(parentId, childEnvelope.getTags().get("ai.operation.parentId"));
    }
}
