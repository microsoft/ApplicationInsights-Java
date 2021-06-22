package com.microsoft.applicationinsights.internal.statsbeat;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class StatsbeatHelperTest {

    private static final Set<String> instrumentations;
    static {
        instrumentations = new HashSet<>();
        instrumentations.add("io.opentelemetry.javaagent.jdbc");
        instrumentations.add("io.opentelemetry.javaagent.tomcat-7.0");
        instrumentations.add("io.opentelemetry.javaagent.http-url-connection");
    }

    private static final long EXPECTED_INSTRUMENTATION = (long)(Math.pow(2, 13) + Math.pow(2, 21) + Math.pow(2, 57)); // Exponents are keys from StatsbeatHelper.INSTRUMENTATION_MAP.)

    private static final Set<Feature> features = Collections.singleton(Feature.JAVA_VENDOR_ZULU);

    private static final long EXPECTED_FEATURE = 2L;

    @Test
    public void testEncodeAndDecodeInstrumentations() {
        String base64EncodedString = Instrumentations.encode(instrumentations);
        assertThat(StatsbeatTestUtils.convertBase64EncodedStringToLong(base64EncodedString)).isEqualTo(EXPECTED_INSTRUMENTATION);
        Set<String> result = StatsbeatTestUtils.decodeInstrumentations(base64EncodedString);
        assertThat(result).isEqualTo(instrumentations);
    }

    @Test
    public void tesEncodeAndDecodeFeature() {
        String base64String = Feature.encode(features);
        assertThat(StatsbeatTestUtils.convertBase64EncodedStringToLong(base64String)).isEqualTo(EXPECTED_FEATURE);
        Set<Feature> result = StatsbeatTestUtils.decodeFeature(base64String);
        assertThat(result).isEqualTo(features);
    }
}
