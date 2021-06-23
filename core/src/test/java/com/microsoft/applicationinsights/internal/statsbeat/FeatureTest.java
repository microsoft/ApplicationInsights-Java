package com.microsoft.applicationinsights.internal.statsbeat;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class FeatureTest {

    private static final Set<Feature> features = Collections.singleton(Feature.JAVA_VENDOR_ZULU);

    private static final long EXPECTED_FEATURE = 2L;

    @Test
    public void tesEncodeAndDecodeFeature() {
        long number = Feature.encode(features);
        assertThat(number).isEqualTo(EXPECTED_FEATURE);
        Set<Feature> result = StatsbeatTestUtils.decodeFeature(number);
        assertThat(result).isEqualTo(features);
    }
}
