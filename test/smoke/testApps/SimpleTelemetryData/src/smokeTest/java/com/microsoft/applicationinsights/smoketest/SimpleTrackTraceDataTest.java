package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;

import org.junit.*;

import static org.junit.Assert.*;

public class SimpleTrackTraceDataTest extends AiSmokeTest {
	@Test
	@TargetUri("/trackTrace?leftOperand=1&rightOperand=2&operator=plus")
	public void testTrackTrace() throws Exception {
		assertEquals(2, mockedIngestion.getCountForType("RequestData"));
		assertEquals(3, mockedIngestion.getCountForType("MessageData"));
		int totalItems = mockedIngestion.getItemCount();
		int expectedItems = 5;
		assertEquals(String.format("There were %d extra telemetry items received.", expectedItems - totalItems),
				expectedItems, totalItems);

		// TODO get trace data envelope and verify value	
		MessageData d = getTelemetryDataForType(0, "MessageData");
		final String expectedMessage = "This is first trace message.";
		assertEquals(expectedMessage, d.getMessage());

		MessageData d2 = getTelemetryDataForType(1, "MessageData");
		final String expectedMessage2 = "This is second trace message.";
		assertEquals(expectedMessage2, d2.getMessage());
		assertEquals(SeverityLevel.Error, d2.getSeverityLevel());

		MessageData d3 = getTelemetryDataForType(2, "MessageData");
		final String expectedMessage3 = "This is third trace message.";
		final String expectedValue = "value";
		assertEquals(expectedMessage3, d3.getMessage());
		assertEquals(SeverityLevel.Information, d3.getSeverityLevel());
		assertEquals(expectedValue, d3.getProperties().get("key"));
	}
}