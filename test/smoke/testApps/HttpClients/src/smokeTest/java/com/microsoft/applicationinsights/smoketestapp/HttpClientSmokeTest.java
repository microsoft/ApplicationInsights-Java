package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.junit.*;

import java.util.List;

import static org.junit.Assert.*;

@UseAgent
public class HttpClientSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri("/apacheHttpClient4")
    public void testApacheHttpClient4() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("HTTP GET", rdd.getName());
        assertEquals("Http", rdd.getType());
        assertEquals("www.bing.com", rdd.getTarget());
        assertEquals("https://www.bing.com/search?q=spaces%20test", rdd.getData());
        assertTrue(rdd.getProperties().isEmpty());
        assertTrue(rdd.getSuccess());

        assertParentChild(rd, rdEnvelope, rddEnvelope, "/HttpClients/*");
    }

    @Test
    @TargetUri("/apacheHttpClient4WithResponseHandler")
    public void testApacheHttpClient4WithResponseHandler() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("HTTP GET", rdd.getName());
        assertEquals("Http", rdd.getType());
        assertEquals("www.bing.com", rdd.getTarget());
        assertEquals("https://www.bing.com/search?q=spaces%20test", rdd.getData());
        assertTrue(rdd.getProperties().isEmpty());
        assertTrue(rdd.getSuccess());

        assertParentChild(rd, rdEnvelope, rddEnvelope, "/HttpClients/*");
    }

    @Test
    @TargetUri("/apacheHttpClient3")
    public void testApacheHttpClient3() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("HTTP GET", rdd.getName());
        assertEquals("Http", rdd.getType());
        assertEquals("www.bing.com", rdd.getTarget());
        assertEquals("https://www.bing.com/search?q=spaces%20test", rdd.getData());
        assertTrue(rdd.getProperties().isEmpty());
        assertTrue(rdd.getSuccess());

        assertParentChild(rd, rdEnvelope, rddEnvelope, "/HttpClients/*");
    }

    @Test
    @TargetUri("/apacheHttpAsyncClient")
    public void testApacheHttpAsyncClient() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("HTTP GET", rdd.getName());
        assertEquals("Http", rdd.getType());
        assertEquals("www.bing.com", rdd.getTarget());
        assertEquals("https://www.bing.com/search?q=spaces%20test", rdd.getData());
        assertTrue(rdd.getProperties().isEmpty());
        assertTrue(rdd.getSuccess());

        assertParentChild(rd, rdEnvelope, rddEnvelope, "/HttpClients/*");
    }

    @Test
    @TargetUri("/okHttp3")
    public void testOkHttp3() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("HTTP GET", rdd.getName());
        assertEquals("Http", rdd.getType());
        assertEquals("www.bing.com", rdd.getTarget());
        assertEquals("https://www.bing.com/search?q=spaces%20test", rdd.getData());
        assertTrue(rdd.getProperties().isEmpty());
        assertTrue(rdd.getSuccess());

        assertParentChild(rd, rdEnvelope, rddEnvelope, "/HttpClients/*");
    }

    @Ignore // OpenTelemetry Auto-Instrumentation does not support OkHttp 2
    @Test
    @TargetUri("/okHttp2")
    public void testOkHttp2() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("HTTP GET", rdd.getName());
        assertEquals("Http", rdd.getType());
        assertEquals("www.bing.com", rdd.getTarget());
        assertEquals("https://www.bing.com/search?q=spaces%20test", rdd.getData());
        assertTrue(rdd.getProperties().isEmpty());
        assertTrue(rdd.getSuccess());

        assertParentChild(rd, rdEnvelope, rddEnvelope, "/HttpClients/*");
    }

    @Test
    @TargetUri("/httpURLConnection")
    public void testHttpURLConnection() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("HTTP GET", rdd.getName());
        assertEquals("Http", rdd.getType());
        assertEquals("www.bing.com", rdd.getTarget());
        assertEquals("https://www.bing.com/search?q=spaces%20test", rdd.getData());
        assertTrue(rdd.getProperties().isEmpty());
        assertTrue(rdd.getSuccess());

        assertParentChild(rd, rdEnvelope, rddEnvelope, "/HttpClients/*");
    }

    @Test
    @TargetUri("/springWebClient")
    public void testSpringWebClient() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("HTTP GET", rdd.getName());
        assertEquals("Http", rdd.getType());
        assertEquals("www.bing.com", rdd.getTarget());
        // TODO investigate why %2520 is captured instead of %20
        assertEquals("https://www.bing.com/search?q=spaces%2520test", rdd.getData());
        assertTrue(rdd.getProperties().isEmpty());
        assertTrue(rdd.getSuccess());

        assertParentChild(rd, rdEnvelope, rddEnvelope, "/HttpClients/*");
    }

    private static void assertParentChild(RequestData rd, Envelope rdEnvelope, Envelope childEnvelope, String operationName) {
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        assertNotNull(operationId);
        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));

        String operationParentId = rdEnvelope.getTags().get("ai.operation.parentId");
        assertNull(operationParentId);

        assertEquals(rd.getId(), childEnvelope.getTags().get("ai.operation.parentId"));

        assertEquals(operationName, rdEnvelope.getTags().get("ai.operation.name"));
        assertNull(childEnvelope.getTags().get("ai.operation.name"));
    }
}
