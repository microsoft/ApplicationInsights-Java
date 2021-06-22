package com.microsoft.applicationinsights.internal.statsbeat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class FeatureStatsbeatTest {

    private FeatureStatsbeat featureStatsbeat;

    @BeforeEach
    public void init() {
        featureStatsbeat = new FeatureStatsbeat();
    }

    @Test
    public void testFeatureList() {
        String javaVendor = System.getProperty("java.vendor");
        Set<Feature> features = Collections.singleton(Feature.fromJavaVendor(javaVendor));
        assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
    }
}
