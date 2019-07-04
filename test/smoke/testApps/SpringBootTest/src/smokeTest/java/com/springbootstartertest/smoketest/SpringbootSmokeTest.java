package com.springbootstartertest.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.DependencyContainer;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import com.microsoft.applicationinsights.smoketest.WithDependencyContainers;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseAgent
public class SpringbootSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri("/basic/trackEvent")
    public void trackEvent() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(2, mockedIngestion.getCountForType("EventData"));

        // TODO get event data envelope and verify value
        EventData d = getTelemetryDataForType(0, "EventData");
        final String name = "EventDataTest";
        assertEquals(name, d.getName());

        EventData d2 = getTelemetryDataForType(1, "EventData");

        final String expectedName = "EventDataPropertyTest";
        final String expectedProperties = "value";
        final Double expectedMetric = 1d;

        assertEquals(expectedName, d2.getName());
        assertEquals(expectedProperties, d2.getProperties().get("key"));
        assertEquals(expectedMetric, d2.getMeasurements().get("key"));
    }

    @Test
    @TargetUri("/throwsException")
    public void testResultCodeWhenRestControllerThrows() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        List<Envelope> exceptionEnvelopeList = mockedIngestion.getItemsEnvelopeDataType("ExceptionData");
        assertEquals(1, exceptionEnvelopeList.size());

        Envelope exceptionEnvelope = exceptionEnvelopeList.get(0);
        RequestData d = getTelemetryDataForType(0, "RequestData");
        String requestOperationId = d.getId();
        assertTrue(requestOperationId.contains(exceptionEnvelope.getTags().
                getOrDefault("ai.operation.id", null)));
    }

    @Test
    @TargetUri("/asyncDependencyCallWithApacheHttpClient4")
    public void testAsyncDependencyCallWithApacheHttpClient4() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }

    @Ignore("Not yet supported")
    @Test
    @TargetUri("/asyncDependencyCallWithApacheHttpClient3")
    public void testAsyncDependencyCallWithApacheHttpClient3() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }

    @Ignore("Not yet supported")
    @Test
    @TargetUri("/asyncDependencyCallWithOkHttp3")
    public void testAsyncDependencyCallWithOkHttp3() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }

    @Test
    @TargetUri("/asyncDependencyCallWithOkHttp2")
    public void testAsyncDependencyCallWithOkHttp2() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        // FIXME correlation for OkHttp is not yet supported.
//        RequestData d = getTelemetryDataForType(0, "RequestData");
//        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
//        String requestOperationId = d.getId();
//        String rddId = rdd.getId();
//        assertTrue(rddId.contains(requestOperationId));
    }

    @Ignore("Not yet supported")
    @Test
    @TargetUri("/asyncDependencyCallWithHttpURLConnection")
    public void testAsyncDependencyCallWithHttpURLConnection() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }
}
