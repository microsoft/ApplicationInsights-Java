package com.microsoft.applicationinsights.smoketest;

import org.junit.Test;

public class SpringBootAutoTest extends AiSmokeTest {

    @Test
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);
    }
}
