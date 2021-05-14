package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class FeatureStatsbeatTest {

    private FeatureStatsbeat featureStatsbeat;

    @Before
    public void init() {
        featureStatsbeat = new FeatureStatsbeat(new TelemetryClient(), Long.MAX_VALUE);
    }

    @Test
    public void testFeatureList() {
        String javaVendor = System.getProperty("java.vendor");
        final Set<Feature> features = Collections.singleton(Feature.fromJavaVendor(javaVendor));
        assertEquals(Feature.encode(features), featureStatsbeat.getFeature());
    }
}
