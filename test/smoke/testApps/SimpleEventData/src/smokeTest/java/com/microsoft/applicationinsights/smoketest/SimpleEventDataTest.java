package com.microsoft.applicationinsights.smoketest;

import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;

import org.junit.*;

import static org.junit.Assert.*;

public class SimpleEventDataTest extends AiSmokeTest {

	@Test
	@TargetUri("/doCalc?leftOperand=1&rightOperand=2&operator=plus")
	public void doCalcSendsRequestDataAndEventData() throws Exception {
		assertEquals(2, mockedIngestion.getCountForType("RequestData"));
		assertEquals(2, mockedIngestion.getCountForType("EventData"));
		int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 4;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems), expectedItems, totalItems);
		// TODO get event data envelope and verify value

		Envelope mEnvelope = mockedIngestion.getItemsByType("EventData").get(0);
		Data<EventData> dHolder = (Data<EventData>) mEnvelope.getData();
		EventData d = dHolder.getBaseData();
		assertEquals("EventDataTest", d.getName());

		Envelope mEnvelope2 = mockedIngestion.getItemsByType("EventData").get(1);
		Data<EventData> dHolder2 = (Data<EventData>) mEnvelope2.getData();
		EventData d2 = dHolder2.getBaseData();
		
		assertEquals("EventDataPropertyTest", d2.getName());
		assertEquals(String.valueOf(100), d2.getProperties().get("price"));
		assertEquals(Double.valueOf(200), d2.getMeasurements().get("score"));
	}
}