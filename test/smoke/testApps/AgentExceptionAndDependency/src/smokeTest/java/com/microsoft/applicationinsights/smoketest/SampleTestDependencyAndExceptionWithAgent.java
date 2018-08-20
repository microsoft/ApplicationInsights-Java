package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;

import org.junit.*;

import static org.junit.Assert.*;

@UseAgent(value="test")
@WithDependencyContainers(@DependencyContainer(value="redis", portMapping="6379"))
public class SampleTestDependencyAndExceptionWithAgent extends AiSmokeTest {

	@Test
	@TargetUri("/trackData")
	public void testDependencyAndExceptionWithAgent() throws Exception {
		assertEquals(2, mockedIngestion.getCountForType("RemoteDependencyData"));
		assertEquals(1, mockedIngestion.getCountForType("ExceptionData"));

		RemoteDependencyData rd = getTelemetryDataForType(1, "RemoteDependencyData");
		assertEquals("Redis", rd.getType());
		assertTrue(rd.getDuration().getTotalMilliseconds() > 0);
	}

}