package com.microsoft.applicationinsights.internal.quickpulse;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DefaultQuickPulseDataFetcherTests {

    @Test
    public void testGetCurrentSdkVersion() {
        DefaultQuickPulseDataFetcher dataFetcher = new DefaultQuickPulseDataFetcher(null, (TelemetryConfiguration) null,
                null, null);
        String sdkVersion = dataFetcher.getCurrentSdkVersion();
        Assert.assertNotNull(sdkVersion);
        Assert.assertNotEquals("java:unknown", sdkVersion);
    }
}
