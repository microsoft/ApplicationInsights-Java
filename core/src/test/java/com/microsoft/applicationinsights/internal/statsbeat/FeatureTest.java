package com.microsoft.applicationinsights.internal.statsbeat;

import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class FeatureTest {

    private static final Set<Feature> features = Collections.singleton(Feature.JAVA_VENDOR_ZULU);

    private static final long EXPECTED_FEATURE = 2L;

    @Test
    public void tesEncodeAndDecodeFeature() {
        long number = Feature.encode(features);
        assertEquals(EXPECTED_FEATURE, number);
        Set<Feature> result = StatsbeatTestUtils.decodeFeature(number);
        assertEquals(features, result);
    }
}
