package com.microsoft.applicationinsights.agent.internal;

import java.io.IOException;
import java.util.Arrays;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.*;

import static org.junit.Assert.*;

public class ConfigurationTest {

    @Test
    public void shouldParse() throws IOException {

        CharSource json = Resources.asCharSource(Resources.getResource("ApplicationInsights.json"), Charsets.UTF_8);
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
        Configuration configuration = jsonAdapter.fromJson(json.read());

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", configuration.connectionString);
        assertEquals("Something Good", configuration.roleName);
        assertEquals("xyz123", configuration.roleInstance);
        assertEquals((Double) 10.0, configuration.experimental.sampling.fixedRate.percentage);
        assertEquals(false, configuration.experimental.liveMetrics.enabled);
        assertEquals(3, configuration.jmxMetrics.size());
        assertEquals("java.lang:type=Threading", configuration.jmxMetrics.get(0).objectName);
        assertEquals("ThreadCount", configuration.jmxMetrics.get(0).attribute);
        assertEquals("Thread Count", configuration.jmxMetrics.get(0).display);
        assertEquals(ImmutableMap.of("__comment",
                Arrays.asList("this sets the explain plan threshold ...", "this is a multi-line comment"),
                "explainPlanThresholdInMS", 20000.0), configuration.experimental.instrumentation.get("jdbc"));
        assertEquals(ImmutableMap.of("enabled", false), configuration.experimental.instrumentation.get("logging"));
    }

    @Test
    public void shouldUseDefaults() throws IOException {

        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
        Configuration configuration = jsonAdapter.fromJson("{}");

        assertEquals(null, configuration.connectionString);
        assertEquals(null, configuration.roleName);
        assertEquals(null, configuration.roleInstance);
        assertEquals(true, configuration.experimental.liveMetrics.enabled);
        assertEquals(0, configuration.jmxMetrics.size());
        assertEquals(0, configuration.experimental.instrumentation.size());
    }
}
