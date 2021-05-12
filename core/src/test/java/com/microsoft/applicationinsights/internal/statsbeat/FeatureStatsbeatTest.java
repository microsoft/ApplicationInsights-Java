package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.DEFAULT_STATSBEAT_INTERVAL;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.FEATURE_STATSBEAT_INTERVAL;
import static org.junit.Assert.assertEquals;

public class FeatureStatsbeatTest {

    private FeatureStatsbeat featureStatsbeat;

    @Before
    public void init() {
        StatsbeatModule.getInstance().initialize(new TelemetryClient(), DEFAULT_STATSBEAT_INTERVAL, FEATURE_STATSBEAT_INTERVAL);
        featureStatsbeat = StatsbeatModule.getInstance().getFeatureStatsbeat();
    }

    @Test
    public void testFeatureList() {
        String javaVendor = System.getProperty("java.vendor");
        final Set<String> features = Collections.singleton(javaVendor);
        long featureLongVal = StatsbeatHelper.encodeFeature(features);
        assertEquals(featureStatsbeat.getFeature(), featureLongVal);
    }

    @Test
    public void testFrequencyInterval() {
        assertEquals(featureStatsbeat.getInterval(), FEATURE_STATSBEAT_INTERVAL);
    }
}
