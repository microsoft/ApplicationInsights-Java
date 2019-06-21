package com.microsoft.applicationinsights.smoketest;

import org.junit.*;

import static org.junit.Assert.*;

public class SpringBootAutoTest extends AiSmokeTest {

	@Test
	@TargetUri("/test")
	public void doMostBasicTest() {
		assertTrue("mocked ingestion has no data", mockedIngestion.hasData());
		assertTrue("mocked ingestion has 0 items", mockedIngestion.getItemCount() > 0);
		
		assertEquals(1, mockedIngestion.getCountForType("RequestData"));
	}
}
