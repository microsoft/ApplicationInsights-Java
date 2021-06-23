package com.microsoft.applicationinsights.internal.statsbeat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class FeatureStatsbeatTest {

    private FeatureStatsbeat featureStatsbeat;
    private String javaVendor;

    @BeforeEach
    public void init() {
        featureStatsbeat = new FeatureStatsbeat();
        javaVendor = System.getProperty("java.vendor");
    }

    @Test
    public void testFeatureList() {
        Set<Feature> features = Collections.singleton(Feature.fromJavaVendor(javaVendor));
        assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
    }

    @Test
    public void testAadEnable() {
        featureStatsbeat.trackAadEnabled(true);
        Set<Feature> features = new HashSet<>();
        features.add(Feature.fromJavaVendor(javaVendor));
        features.add(Feature.AAD_ON);
        assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
    }

    @Test
    public void testAadDisable() {
        featureStatsbeat.trackAadEnabled(false);
        Set<Feature> features = new HashSet<>();
        features.add(Feature.fromJavaVendor(javaVendor));
        assertThat(featureStatsbeat.getFeature()).isEqualTo(Feature.encode(features));
    }
}
