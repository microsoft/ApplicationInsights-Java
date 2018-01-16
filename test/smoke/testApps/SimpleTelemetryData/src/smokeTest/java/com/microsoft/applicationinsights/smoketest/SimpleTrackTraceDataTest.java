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
		MessageData d = GetTraceData(0);
		final String expectedMessage = "This is first trace message.";
		assertEquals(expectedMessage, d.getMessage());
	}

	@Test
	public void testTrackTraceWithSeverityLevel(){
		MessageData d = GetTraceData(1);
		final String expectedMessage = "This is second trace message.";		
		assertEquals(expectedMessage, d.getMessage());
		assertEquals(SeverityLevel.Error, d.getSeverityLevel());
	}

	@Test
	public void testTrackTraceWithProperties(){
		MessageData d = GetTraceData(2);
		final String expectedMessage = "This is third trace message.";
		final String expectedValue = "Test";
		assertEquals(expectedMessage, d.getMessage());
		assertEquals(SeverityLevel.Information, d.getSeverityLevel());
		assertEquals(expectedValue, d.getProperties().get("key"));
	}

	private MessageData GetTraceData(int index) {
		Envelope mEnvelope = mockedIngestion.getItemsByType("MessageData").get(index);
		Data<MessageData> dHolder = (Data<MessageData>) mEnvelope.getData();
		MessageData d = dHolder.getBaseData();
		return d;
	}
}