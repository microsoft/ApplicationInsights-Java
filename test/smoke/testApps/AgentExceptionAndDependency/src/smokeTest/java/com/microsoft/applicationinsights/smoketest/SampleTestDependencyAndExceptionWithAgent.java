package com.microsoft.applicationinsights.smoketest;

import java.util.List;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.telemetry.Duration;

import org.junit.*;

import static org.junit.Assert.*;

@UseAgent
public class SampleTestDependencyAndExceptionWithAgent extends AiSmokeTest {

	@Test
	@TargetUri("/trackData")
	public void testDependencyAndExceptionWithAgent() throws Exception {

		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
		assertEquals(1, mockedIngestion.getCountForType("RemoteDependencyData"));
		assertEquals(1, mockedIngestion.getCountForType("ExceptionData"));

		ExceptionData exceptionData = getTelemetryDataForType(0, "ExceptionData");
		ExceptionDetails exceptionDetails = getExceptionDetails(exceptionData);
		assertEquals("This is track exception.", exceptionDetails.getMessage());

		RemoteDependencyData remoteDependencyData = getTelemetryDataForType(0, "RemoteDependencyData");
		assertEquals("AgentDependencyTest", remoteDependencyData.getName());
		assertEquals("commandName", remoteDependencyData.getData());
		assertEquals(new Duration(0, 0, 1, 1, 1), remoteDependencyData.getDuration());
		assertEquals(true, remoteDependencyData.getSuccess());
	}

	private ExceptionDetails getExceptionDetails(ExceptionData exceptionData) {
		List<ExceptionDetails> details = exceptionData.getExceptions();
		ExceptionDetails ex = details.get(0);
		return ex;
	}
}