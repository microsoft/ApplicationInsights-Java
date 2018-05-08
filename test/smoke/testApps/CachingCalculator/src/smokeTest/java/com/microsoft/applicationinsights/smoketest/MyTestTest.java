package com.microsoft.applicationinsights.smoketest;

import org.junit.*;

import static org.junit.Assert.*;

@UseAgent
@WithDependencyContainers(@DependencyContainer("redis"))
public class MyTestTest extends AiSmokeTest {

	@Test
	@TargetUri("/index.jsp")
	public void doCalcSendsRequestDataAndMetricData() throws Exception {
		assertTrue("mocked ingestion has no data", mockedIngestion.hasData());
		assertTrue("mocked ingestion has 0 items", mockedIngestion.getItemCount() > 0);
		
		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
		assertEquals(3, mockedIngestion.getCountForType("RemoteDependencyData"));
	}
}