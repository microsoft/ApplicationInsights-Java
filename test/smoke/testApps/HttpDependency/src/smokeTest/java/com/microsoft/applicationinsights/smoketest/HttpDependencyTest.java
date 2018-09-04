package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;

import org.junit.Test;

@UseAgent
public class HttpDependencyTest extends AiSmokeTest {

    @Test
    @TargetUri("/httpDependency")
    public void testHttpDependency() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));

        //Now only can get 3 remote dependency. Can not collect non-exist url failed http depnedncy if used Apache http client
        //assertEquals(4, mockedIngestion.getCountForType("RemoteDependencyData"));

        assertEquals(2, mockedIngestion.getCountForType("ExceptionData"));

        RemoteDependencyData rd = getTelemetryDataForType(0, "RemoteDependencyData");
        assertEquals("200", rd.getResultCode());
        assertEquals(true, rd.getSuccess());
        assertTrue(rd.getDuration().getTotalMilliseconds() > 0);
        assertEquals("https://www.bing.com", rd.getProperties().get("URI"));

        // Can't get result code if used OkHttpClient
        RemoteDependencyData rd1 = getTelemetryDataForType(1, "RemoteDependencyData");
        assertEquals("https://www.microsoft.com/", rd1.getName());
        assertEquals(true, rd1.getSuccess());
        assertTrue(rd1.getDuration().getTotalMilliseconds() > 0);

        RemoteDependencyData rd2 = getTelemetryDataForType(2, "RemoteDependencyData");
        assertEquals("https://www.microsoftabc.com/", rd2.getName());
        assertEquals(false, rd2.getSuccess());
        assertTrue(rd2.getDuration().getTotalMilliseconds() > 0);

        ExceptionData ed = getTelemetryDataForType(0, "ExceptionData");
        ExceptionDetails eDetails = getExceptionDetails(ed);
        assertTrue(eDetails.getMessage().contains("www.microsoftabc.com"));

        ExceptionData ed1 = getTelemetryDataForType(1, "ExceptionData");
        ExceptionDetails eDetails1 = getExceptionDetails(ed1);
        assertTrue(eDetails1.getMessage().contains("www.bingxxxxx.com"));

    }

    private ExceptionDetails getExceptionDetails(ExceptionData exceptionData) {
        List<ExceptionDetails> details = exceptionData.getExceptions();
        ExceptionDetails ex = details.get(0);
        return ex;
	}

}