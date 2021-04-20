package com.microsoft.applicationinsights.internal.statsbeat;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class StatsbeatHelperTest {

    private static final Set<String> instrumentations = new HashSet<String>() {{
        add("opentelemetry-javaagent-jdbc");
        add("opentelemetry-javaagent-tomcat-7.0");
        add("opentelemetry-javaagent-http-url-connection");
    }};

    private long instrumentation = 4503599627898880L;

    @Test
    public void testEncodeAndDecodeInstrumentations() {
        long num = StatsbeatHelper.encodeInstrumentations(instrumentations);
        assertEquals(instrumentation, num);
        Set<String> result = StatsbeatHelper.decodeInstrumentations(num);
        assertEquals(instrumentations, result);
    }
}
