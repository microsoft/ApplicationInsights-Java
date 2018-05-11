package com.springbootstartertest.smoketest;

import static org.junit.Assert.assertEquals;

import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class SpringbootSmokeTest extends AiSmokeTest{

	@Test
	@TargetUri("/basic/trackEvent")
	public void trackEvent() throws Exception {
		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
//		assertEquals(2, mockedIngestion.getCountForType("EventData"));
		int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 1;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
			expectedItems, totalItems);

		// TODO get event data envelope and verify value
//		EventData d = getTelemetryDataForType(0, "EventData");
//		final String name = "EventDataTest";
//		assertEquals(name, d.getName());
//
//		EventData d2 = getTelemetryDataForType(1, "EventData");
//
//		final String expectedname = "EventDataPropertyTest";
//		final String expectedProperties = "value";
//		final Double expectedMetrice = 1d;
//
//		assertEquals(expectedname, d2.getName());
//		assertEquals(expectedProperties, d2.getProperties().get("key"));
//		assertEquals(expectedMetrice, d2.getMeasurements().get("key"));
	}
}
