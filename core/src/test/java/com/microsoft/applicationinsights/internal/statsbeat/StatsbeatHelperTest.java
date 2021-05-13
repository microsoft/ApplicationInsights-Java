package com.microsoft.applicationinsights.internal.statsbeat;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_ZULU;
import static org.junit.Assert.assertEquals;

public class StatsbeatHelperTest {

    private static final Set<String> instrumentations;
    static {
        instrumentations = new HashSet<>();
        instrumentations.add("io.opentelemetry.javaagent.jdbc");
        instrumentations.add("io.opentelemetry.javaagent.tomcat-7.0");
        instrumentations.add("io.opentelemetry.javaagent.http-url-connection");
    }

    private static final long EXPECTED_INSTRUMENTATION = (long)(Math.pow(2, 13) + Math.pow(2, 21) + Math.pow(2, 57)); // Exponents are keys from StatsbeatHelper.INSTRUMENTATION_MAP.)

    private static final Set<String> features = Collections.singleton(JAVA_VENDOR_ZULU);

    private static final long EXPECTED_FEATURE = 2L;

    @Test
    public void testEncodeAndDecodeInstrumentations() {
        long num = StatsbeatHelper.encodeInstrumentations(instrumentations);
        assertEquals(EXPECTED_INSTRUMENTATION, num);
        Set<String> result = StatsbeatTestUtils.decodeInstrumentations(num);
        assertEquals(instrumentations, result);
    }

    @Test
    public void tesEncodeAndDecodeFeature() {
        long num = StatsbeatHelper.encodeFeature(features);
        assertEquals(EXPECTED_FEATURE, num);
        Set<String> result = StatsbeatTestUtils.decodeFeature(num);
        assertEquals(features, result);
    }
}
