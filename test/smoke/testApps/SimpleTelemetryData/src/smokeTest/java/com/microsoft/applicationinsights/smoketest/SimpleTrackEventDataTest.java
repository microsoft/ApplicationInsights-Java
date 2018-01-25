package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.EventData;

import org.junit.*;

import static org.junit.Assert.*;

public class SimpleTrackEventDataTest extends AiSmokeTest {

	@Test
	@TargetUri("/doCalc?leftOperand=1&rightOperand=2&operator=plus")
	public void testTrackEvent() throws Exception {
		assertEquals(2, mockedIngestion.getCountForType("RequestData"));
		assertEquals(2, mockedIngestion.getCountForType("EventData"));
		int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 4;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
				expectedItems, totalItems);

		// TODO get event data envelope and verify value
		EventData d = getTelemetryDataForType(0, "EventData");
		final String name = "EventDataTest";
		assertEquals(name, d.getName());

		EventData d2 = getTelemetryDataForType(1, "EventData");

		final String expectedname = "EventDataPropertyTest";
		final String expectedProperties = "value";
		final Double expectedMetrice = 1d;

		assertEquals(expectedname, d2.getName());
		assertEquals(expectedProperties, d2.getProperties().get("key"));
		assertEquals(expectedMetrice, d2.getMeasurements().get("key"));
	}
}