package com.springbootstartertest.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.*;

import java.util.List;

import org.junit.Test;

@UseAgent
// NOTE this test doesn't need dependency containers, but currently all test classes need to specify the dependency
// containers since they are set up statically in AiSmokeTest
@WithDependencyContainers({
        @DependencyContainer(
                value = "mysql:5",
                environmentVariables = {"MYSQL_ROOT_PASSWORD=password"},
                portMapping = "3306",
                hostnameEnvironmentVariable = "MYSQL"),
        @DependencyContainer(
                value = "postgres:11",
                portMapping = "5432",
                hostnameEnvironmentVariable = "POSTGRES"),
        @DependencyContainer(
                value = "mcr.microsoft.com/mssql/server:2017-latest",
                environmentVariables = {"ACCEPT_EULA=Y", "SA_PASSWORD=Password1"},
                portMapping = "1433",
                hostnameEnvironmentVariable = "SQLSERVER")
})
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
    @TargetUri("/asyncDependencyCallWithApacheHttpClient4")
    public void testAsyncDependencyCallWithApacheHttpClient4() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0,"RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }

    @Test
    @TargetUri("/asyncDependencyCallWithApacheHttpClient3")
    public void testAsyncDependencyCallWithApacheHttpClient3() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0,"RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }

    @Test
    @TargetUri("/asyncDependencyCallWithOkHttp3")
    public void testAsyncDependencyCallWithOkHttp3() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0,"RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }

    @Test
    @TargetUri("/asyncDependencyCallWithOkHttp2")
    public void testAsyncDependencyCallWithOkHttp2() {
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
        assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
        RequestData d = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = getTelemetryDataForType(0,"RemoteDependencyData");
        String requestOperationId = d.getId();
        String rddId = rdd.getId();
        assertTrue(rddId.contains(requestOperationId));
    }
}
