package com.microsoft.applicationinsights.smoketest;

import org.junit.*;

@UseAgent
public class SpringBootAutoTest extends AiSmokeTest {

    @Ignore
    @TargetUri("/test")
    public void doMostBasicTest() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);
    }
}
