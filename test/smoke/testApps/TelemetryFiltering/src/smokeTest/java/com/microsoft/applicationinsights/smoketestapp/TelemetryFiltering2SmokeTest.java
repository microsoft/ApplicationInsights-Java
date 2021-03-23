package com.microsoft.applicationinsights.smoketestapp;

import com.google.common.base.Stopwatch;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.junit.Test;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.*;

@UseAgent("telemetryfiltering2")
public class TelemetryFiltering2SmokeTest extends AiSmokeTest {

    @Test
    @TargetUri(value = "/login", callCount = 100)
    public void testSampling() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (mockedIngestion.getCountForType("RequestData") < 100
                && mockedIngestion.getCountForType("RemoteDependencyData") < 100
                && stopwatch.elapsed(SECONDS) < 10) {
        }
        assertEquals(100, mockedIngestion.getCountForType("RequestData"));
        assertEquals(100, mockedIngestion.getCountForType("RemoteDependencyData"));
    }
}
