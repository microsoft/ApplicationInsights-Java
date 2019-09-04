package com.microsoft.applicationinsights.smoketest;

import org.junit.Test;

public class WebAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);
    }
}
