package com.springbootstartertest.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import java.util.List;
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

	@Test
	@TargetUri("/throwsException")
	public void testResultCodeWhenRestControllerThrows() {
		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
		List<Envelope> exceptionEnvelopeList = mockedIngestion.getItemsEnvelopeDataType("ExceptionData");
		assertEquals(1, exceptionEnvelopeList.size());

		Envelope exceptionEnvelope = exceptionEnvelopeList.get(0);
		RequestData d = getTelemetryDataForType(0, "RequestData");
		String requestOperationId = d.getId();
		assertTrue(requestOperationId.contains(exceptionEnvelope.getTags().
			getOrDefault("ai.operation.id", null)));
	}

	@Test
	@TargetUri("/asyncDependencyCall")
	public void testAsyncDependencyCall() {
		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
		assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
		RequestData d = getTelemetryDataForType(0, "RequestData");
//		RemoteDependencyData rdd = getTelemetryDataForType(0,"RemoteDependencyData");
		String requestOperationId = d.getId();

		List<Envelope> rddDataList = mockedIngestion.getItemsEnvelopeDataType("RemoteDependencyData");
		Envelope rddEnv = rddDataList.get(0);

    	System.out.println("*****Request Id in smoke test is: " + requestOperationId);
    	System.out.println("*****DependencyTelemetry Operation Id in smoke test is: " +
			rddEnv.getTags().getOrDefault("ai.operation.id", null));
		//assertEquals(requestOperationId + "1.", rdd.getId());
	}
}
