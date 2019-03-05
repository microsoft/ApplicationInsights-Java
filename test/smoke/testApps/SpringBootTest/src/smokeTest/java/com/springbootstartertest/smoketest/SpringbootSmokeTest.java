package com.springbootstartertest.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.localforwarder.library.inputs.contracts.Request;
import org.junit.Test;

@UseAgent
public class SpringbootSmokeTest extends AiSmokeTest{

	@Test
	@TargetUri("/basic/trackEvent")
	public void trackEvent() throws Exception {
		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
		assertEquals(2, mockedIngestion.getCountForType("EventData"));
		int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 3;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
			expectedItems, totalItems);

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

//	@Test
//	@TargetUri("/throwsException")
//	public void testResultCodeWhenRestControllerThrows() {
//		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
//		RequestData d = getTelemetryDataForType(0, "RequestData");
//		final String expectedResponseCode = "500";
//		assertEquals(expectedResponseCode, d.getResponseCode());
//		assertFalse( d.getSuccess());
//	}

	@Test
	@TargetUri("/asyncDependencyCall")
	public void testAsyncDependencyCall() {
		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
		assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
	}
}
