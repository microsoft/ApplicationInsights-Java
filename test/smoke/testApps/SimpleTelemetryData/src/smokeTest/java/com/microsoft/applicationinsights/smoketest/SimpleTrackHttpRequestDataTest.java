package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;

import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.telemetry.Duration;

import org.junit.Test;

public class SimpleTrackHttpRequestDataTest extends AiSmokeTest {
    @Test
    @TargetUri("/trackHttpRequest?leftOperand=1&rightOperand=2&operator=plus")
    public void testHttpRequest() throws Exception {
        assertEquals(4, mockedIngestion.getCountForType("RequestData"));

        int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 4;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
                expectedItems, totalItems);
                
        // TODO get HttpRequest data envelope and verify value
        RequestData d = getTelemetryDataForType(1, "RequestData");
        
        final String expectedName = "HttpRequestDataTest";
        final String expectedResponseCode = "200";

        assertEquals(expectedName, d.getName());
        assertEquals(expectedResponseCode, d.getResponseCode());
        assertEquals(new Duration(4711), d.getDuration());
        assertEquals(true, d.getSuccess());

        RequestData d1 = getTelemetryDataForType(2, "RequestData");

        final String expectedName1 = "PingTest";
        final String expectedResponseCode1 = "200";
        final String expectedURL = "http://tempuri.org/ping";

        assertEquals(expectedName1, d1.getName());
        assertEquals(expectedResponseCode1, d1.getResponseCode());
        assertEquals(new Duration(1), d1.getDuration());
        assertEquals(true, d1.getSuccess());
        assertEquals(expectedURL, d1.getUrl());
    }
}