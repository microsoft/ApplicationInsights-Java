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
public class KafkaTest extends AiSmokeTest {

    @Test
    @TargetUri("/sendMessage")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);
        List<Envelope> rddList = mockedIngestion.waitForItemsInRequest("RemoteDependencyData", 2);

        Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /sendMessage");
        Envelope rdEnvelope2 = getRequestEnvelope(rdList, "mytopic");
        Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "mytopic");
        Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "GET /");

        RequestData rd1 = (RequestData) ((Data) rdEnvelope1.getData()).getBaseData();
        RequestData rd2 = (RequestData) ((Data) rdEnvelope2.getData()).getBaseData();
        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data) rddEnvelope1.getData()).getBaseData();

        assertParentChild(rd1.getId(), rdEnvelope1, rddEnvelope1);
        assertParentChild(rdd1.getId(), rddEnvelope1, rdEnvelope2);
        assertParentChild(rd2.getId(), rdEnvelope2, rddEnvelope2);
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
