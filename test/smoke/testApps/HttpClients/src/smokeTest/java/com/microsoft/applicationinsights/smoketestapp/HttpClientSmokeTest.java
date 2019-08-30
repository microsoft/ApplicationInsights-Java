package com.microsoft.applicationinsights.smoketestapp;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
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
    public void testAsyncDependencyCallWithApacheHttpClient4() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        RequestData rd = (RequestData) ((Data) rdList.get(0).getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddList.get(0).getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());
        assertTrue(rdd.getId().contains(rd.getId()));
    }

    @Test
    @TargetUri("/apacheHttpClient3")
    public void testAsyncDependencyCallWithApacheHttpClient3() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        RequestData rd = (RequestData) ((Data) rdList.get(0).getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddList.get(0).getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());
        assertTrue(rdd.getId().contains(rd.getId()));
    }

    @Test
    @TargetUri("/okHttp3")
    public void testAsyncDependencyCallWithOkHttp3() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        RequestData rd = (RequestData) ((Data) rdList.get(0).getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddList.get(0).getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());
        assertTrue(rdd.getId().contains(rd.getId()));
    }

    @Test
    @TargetUri("/okHttp2")
    public void testAsyncDependencyCallWithOkHttp2() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        RequestData rd = (RequestData) ((Data) rdList.get(0).getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddList.get(0).getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());
        assertTrue(rdd.getId().contains(rd.getId()));
    }

    @Test
    @TargetUri("/httpURLConnection")
    public void testAsyncDependencyCallWithHttpURLConnection() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItems("RemoteDependencyData", 1);

        RequestData rd = (RequestData) ((Data) rdList.get(0).getData()).getBaseData();
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddList.get(0).getData()).getBaseData();

        assertTrue(rd.getSuccess());
        assertEquals("GET /", rdd.getName());
        assertEquals("www.bing.com:-1 | www.bing.com", rdd.getTarget());
        assertTrue(rdd.getId().contains(rd.getId()));
    }
}
