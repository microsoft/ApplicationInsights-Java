package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.statsbeat.Constants.Feature;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.FEATURE_STATSBEAT_INTERVAL;
import static org.junit.Assert.assertEquals;

public class FeatureStatsbeatTest {

    private FeatureStatsbeat featureStatsbeat;

    @Before
    public void init() {
        featureStatsbeat = new FeatureStatsbeat(new TelemetryClient(), FEATURE_STATSBEAT_INTERVAL);
    }

    @Test
    public void testFeatureList() {
        String javaVendor = System.getProperty("java.vendor");
        final Set<Feature> features = Collections.singleton(Feature.fromJavaVendor(javaVendor));
        long featureLongVal = StatsbeatHelper.encodeFeature(features);
        assertEquals(featureLongVal, featureStatsbeat.getFeature());
    }
}
