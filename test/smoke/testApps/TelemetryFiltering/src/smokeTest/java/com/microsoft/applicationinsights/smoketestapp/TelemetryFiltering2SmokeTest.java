package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.junit.Test;

@UseAgent("telemetryfiltering2")
public class TelemetryFiltering2SmokeTest extends AiSmokeTest {

    @Test
    @TargetUri(value = "/login", callCount = 100)
    public void testSampling() throws Exception {
        mockedIngestion.waitForItems("RequestData", 100);
        mockedIngestion.waitForItems("RemoteDependencyData", 100);
    }
}
