package com.microsoft.applicationinsights.smoketestapp;

import java.util.List;

import com.google.common.base.Stopwatch;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.junit.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@UseAgent("telemetryfiltering")
public class TelemetryFilteringSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri(value = "/health-check", callCount = 100)
    public void testSampling() throws Exception {
        // super super low chance that number of sampled requests is less than 25
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (mockedIngestion.getCountForType("RequestData") < 25 && stopwatch.elapsed(SECONDS) < 10) {
        }
        // wait ten more seconds to before checking that we didn't receive too many
        Thread.sleep(SECONDS.toMillis(10));
        int requestCount = mockedIngestion.getCountForType("RequestData");
        int dependencyCount = mockedIngestion.getCountForType("RemoteDependencyData");
        // super super low chance that number of sampled requests/dependencies
        // is less than 25 or greater than 75
        assertThat(requestCount, greaterThanOrEqualTo(25));
        assertThat(dependencyCount, greaterThanOrEqualTo(25));
        assertThat(requestCount, lessThanOrEqualTo(75));
        assertThat(dependencyCount, lessThanOrEqualTo(75));
    }

    @Test
    @TargetUri("/noisy-jdbc")
    public void testNoisyJdbc() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Thread.sleep(10000);
        assertEquals(0, mockedIngestion.getCountForType("RemoteDependencyData"));

        Envelope rdEnvelope = rdList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
    }

    @Test
    @TargetUri("/regular-jdbc")
    public void testRegularJdbc() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");

        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope = rddList.get(0);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("SQL", rdd.getType());
        assertEquals("testdb", rdd.getTarget());
        assertEquals("select * from abc", rdd.getName());
        assertEquals("select * from abc", rdd.getData());
        assertTrue(rdd.getSuccess());

        assertParentChild(rd, rdEnvelope, rddEnvelope, "GET /TelemetryFiltering/*");
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
