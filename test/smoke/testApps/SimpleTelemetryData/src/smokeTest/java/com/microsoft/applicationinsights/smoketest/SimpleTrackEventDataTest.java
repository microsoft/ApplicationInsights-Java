package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;

import org.junit.*;

import static org.junit.Assert.*;

public class SimpleTrackEventDataTest extends AiSmokeTest {
	@Test
	public void testTrackEventWithName(){
		EventData d = getTelemetryTypeData(0, "EventData");
		final String name = "EventDataTest";
		assertEquals(name, d.getName());
	}

	@Test
	public void testTrackEventWithPropertiesAndMetrics(){
		EventData d = getTelemetryTypeData(1, "EventData");

		final String name = "EventDataPropertyTest";
		final String expectedProperties = "value";
		final Double expectedMetrice = 1d;
		
		assertEquals(name, d.getName());
		assertEquals(expectedProperties, d.getProperties().get("key"));
		assertEquals(expectedMetrice, d.getMeasurements().get("key"));
	}	

	@Test
    public void testTrackEventCount(){
        int actualItems = mockedIngestion.getCountForType("EventData");
		int expectedItems = 2;
        assertEquals(String.format("There were %d extra EventData received.", actualItems - expectedItems) , expectedItems, actualItems);
    }
}