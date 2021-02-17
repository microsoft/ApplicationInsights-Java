package com.microsoft.applicationinsights.smoketest;

import org.junit.*;

import static org.junit.Assert.*;

@UseAgent("disabled_micrometer")
public class MicrometerDisabledTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);

        // sleep a bit and make sure no micrometer metrics are reported
        Thread.sleep(10000);
        assertEquals(0, mockedIngestion.getCountForType("MetricData"));
    }
}
