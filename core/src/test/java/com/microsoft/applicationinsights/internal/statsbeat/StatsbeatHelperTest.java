package com.microsoft.applicationinsights.internal.statsbeat;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_ZULU;
import static org.junit.Assert.assertEquals;

public class StatsbeatHelperTest {

    private static final Set<String> instrumentations = new HashSet<String>() {{
        add("io.opentelemetry.javaagent.jdbc");
        add("io.opentelemetry.javaagent.tomcat-7.0");
        add("io.opentelemetry.javaagent.http-url-connection");
    }};

    private long instrumentation = 144115188077961216L;

    private static final Set<String> features = new HashSet<String>() {{
        add(JAVA_VENDOR_ZULU);
    }};

    private long feature = 2L;

    @Test
    public void testEncodeAndDecodeInstrumentations() {
        long num = StatsbeatHelper.encodeInstrumentations(instrumentations);
        assertEquals(instrumentation, num);
        Set<String> result = StatsbeatHelper.decodeInstrumentations(num);
        assertEquals(instrumentations, result);
    }

    @Test
    public void tesEncodeAndDecodeFeature() {
        long num = StatsbeatHelper.encodeFeature(features);
        assertEquals(feature, num);
        Set<String> result = StatsbeatHelper.decodeFeature(num);
        assertEquals(features, result);
    }
}
