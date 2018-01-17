package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;

import org.junit.*;

import static org.junit.Assert.*;

public class SimpleTrackTraceDataTest extends AiSmokeTest {
	@Test
	public void testTrackTraceWithName(){
		MessageData d = getTelemetryTypeData(0, "MessageData");
		final String expectedMessage = "This is first trace message.";
		assertEquals(expectedMessage, d.getMessage());
	}

	@Test
	public void testTrackTraceWithSeverityLevel(){
		MessageData d = getTelemetryTypeData(1, "MessageData");
		final String expectedMessage = "This is second trace message.";		
		assertEquals(expectedMessage, d.getMessage());
		assertEquals(SeverityLevel.Error, d.getSeverityLevel());
	}

	@Test
	public void testTrackTraceWithProperties(){
		MessageData d = getTelemetryTypeData(2, "MessageData");
		final String expectedMessage = "This is third trace message.";
		final String expectedValue = "value";
		assertEquals(expectedMessage, d.getMessage());
		assertEquals(SeverityLevel.Information, d.getSeverityLevel());
		assertEquals(expectedValue, d.getProperties().get("key"));
	}

	@Test
    public void testTrackTraceCount(){
        int actualItems = mockedIngestion.getCountForType("MessageData");
		int expectedItems = 3;
        assertEquals(String.format("There were %d extra MessageData received.", actualItems - expectedItems) , expectedItems, actualItems);
    }

}