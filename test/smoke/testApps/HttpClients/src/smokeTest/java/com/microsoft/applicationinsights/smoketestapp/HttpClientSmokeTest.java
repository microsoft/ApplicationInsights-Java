package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseAgent
public class HttpClientSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri("/apacheHttpClient4")
    public void testAsyncDependencyCallWithApacheHttpClient4() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());
        assertTrue(rdd.getId().contains(d.getId()));
    }

    @Test
    @TargetUri("/apacheHttpClient3")
    public void testAsyncDependencyCallWithApacheHttpClient3() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());
        assertTrue(rdd.getId().contains(d.getId()));
    }

    @Test
    @TargetUri("/okHttp3")
    public void testAsyncDependencyCallWithOkHttp3() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());
        assertTrue(rdd.getId().contains(d.getId()));
    }

    @Test
    @TargetUri("/okHttp2")
    public void testAsyncDependencyCallWithOkHttp2() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());
        assertTrue(rdd.getId().contains(d.getId()));
    }

    @Test
    @TargetUri("/httpURLConnection")
    public void testAsyncDependencyCallWithHttpURLConnection() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());
        assertTrue(rdd.getId().contains(d.getId()));
    }
}
