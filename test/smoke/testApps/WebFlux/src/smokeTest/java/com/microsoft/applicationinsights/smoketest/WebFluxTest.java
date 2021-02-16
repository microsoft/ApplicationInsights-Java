package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

@UseAgent
public class WebFluxTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");

        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("/test/**", rd.getName());
        assertEquals("200", rd.getResponseCode());
    }

    @Test
    @TargetUri("/exception")
    public void testException() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");

        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertFalse(rd.getSuccess());
        assertEquals("500", rd.getResponseCode());
    }

    @Test
    @TargetUri("/futureException")
    public void testFutureException() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");

        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertFalse(rd.getSuccess());
        assertEquals("500", rd.getResponseCode());
    }
}
