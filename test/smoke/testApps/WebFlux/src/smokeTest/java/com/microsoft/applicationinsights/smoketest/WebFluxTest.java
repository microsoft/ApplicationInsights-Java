package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

@UseAgent
public class WebFluxTest extends AiSmokeTest {

    @Rule
    public Retry retry = new Retry(3);

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");

        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertEquals("GET /test/**", rd.getName());
        assertEquals("200", rd.getResponseCode());
        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());
    }

    @Test
    @TargetUri("/exception")
    public void testException() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");

        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertEquals("GET /exception", rd.getName());
        assertEquals("500", rd.getResponseCode());
        assertTrue(rd.getProperties().isEmpty());
        assertFalse(rd.getSuccess());
    }

    @Test
    @TargetUri("/futureException")
    public void testFutureException() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");

        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertEquals("GET /futureException", rd.getName());
        assertEquals("500", rd.getResponseCode());
        assertTrue(rd.getProperties().isEmpty());
        assertFalse(rd.getSuccess());
    }
}
