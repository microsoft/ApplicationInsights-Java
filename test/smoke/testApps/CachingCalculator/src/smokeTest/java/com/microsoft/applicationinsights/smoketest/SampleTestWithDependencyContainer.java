package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import org.junit.*;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

@UseAgent
@WithDependencyContainers(@DependencyContainer(value="redis", portMapping="6379"))
public class SampleTestWithDependencyContainer extends AiSmokeTest {

    @Test
    @TargetUri("/index.jsp")
    public void doCalcSendsRequestDataAndMetricData() {
        List<Envelope> rdList = mockedIngestion.getItemsEnvelopeDataType("RequestData");
        List<Envelope> rddList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");

        assertThat(rdList, hasSize(1));
        assertThat(rddList, hasSize(1));

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);

        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertEquals("Redis", rdd.getType());
        assertTrue(rdd.getSuccess());

        assertSameOperationId(rdEnvelope, rddEnvelope);
    }

    private static void assertSameOperationId(Envelope rdEnvelope, Envelope rddEnvelope) {
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        String operationParentId = rdEnvelope.getTags().get("ai.operation.parentId");

        assertNotNull(operationId);
        assertNotNull(operationParentId);

        assertEquals(operationId, rddEnvelope.getTags().get("ai.operation.id"));
        assertEquals(operationParentId, rddEnvelope.getTags().get("ai.operation.parentId"));
    }
}