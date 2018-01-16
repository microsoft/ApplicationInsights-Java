package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;

import org.junit.*;

import static org.junit.Assert.*;

public class SimpleTrackEventDataTest extends AiSmokeTest {
	@Test
	public void testTrackEventWithName(){
		EventData d = GetEventData(0);
		final String name = "EventDataTest";
		assertEquals(name, d.getName());
	}

	@Test
	public void testTrackEventWithPropertiesAndMetrics(){
		EventData d = GetEventData(1);
		final String name = "EventDataPropertyTest";
		assertEquals(name, d.getName());
		assertEquals(String.valueOf(100), d.getProperties().get("price"));
		assertEquals(Double.valueOf(200), d.getMeasurements().get("score"));
	}	

	private EventData GetEventData(int index) {
		Envelope mEnvelope = mockedIngestion.getItemsByType("EventData").get(index);
		Data<EventData> dHolder = (Data<EventData>) mEnvelope.getData();
		EventData d = dHolder.getBaseData();
		return d;
	}

}