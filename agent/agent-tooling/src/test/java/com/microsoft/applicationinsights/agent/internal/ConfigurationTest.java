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
        assertEquals((Double) 9.0, configuration.experimental.sampling.fixedRate.default_);
        assertEquals((Double) 8.0, configuration.experimental.sampling.fixedRate.requests);
        assertEquals((Double) 7.0, configuration.experimental.sampling.fixedRate.dependencies);
        assertEquals((Double) 6.0, configuration.experimental.sampling.fixedRate.exceptions);
        assertEquals((Double) 5.0, configuration.experimental.sampling.fixedRate.traces);
        assertEquals((Double) 4.0, configuration.experimental.sampling.fixedRate.customEvents);
        assertEquals((Double) 3.0, configuration.experimental.sampling.fixedRate.pageViews);
        assertEquals(false, configuration.experimental.distributedTracing.outboundEnabled);
        assertEquals(false, configuration.experimental.distributedTracing.requestIdCompatEnabled);
        assertEquals(false, configuration.experimental.liveMetrics.enabled);
        assertEquals(3, configuration.jmxMetrics.size());
        assertEquals("java.lang:type=Threading", configuration.jmxMetrics.get(0).objectName);
        assertEquals("ThreadCount", configuration.jmxMetrics.get(0).attribute);
        assertEquals("Thread Count", configuration.jmxMetrics.get(0).display);
        assertEquals(ImmutableMap.of("__comment",
                Arrays.asList("this sets the explain plan threshold ...", "this is a multi-line comment"),
                "explainPlanThresholdInMS", 20000.0), configuration.experimental.instrumentation.get("jdbc"));
        assertEquals(ImmutableMap.of("enabled", false), configuration.experimental.instrumentation.get("logging"));
        assertEquals(2, configuration.experimental.customInstrumentation.size());
        assertEquals("com.example.MyObject", configuration.experimental.customInstrumentation.get(0).className);
        assertEquals("doSomething", configuration.experimental.customInstrumentation.get(0).methodName);
    }

    @Test
    public void shouldUseDefaults() throws IOException {

        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
        Configuration configuration = jsonAdapter.fromJson("{}");

        assertEquals(null, configuration.connectionString);
        assertEquals(null, configuration.roleName);
        assertEquals(null, configuration.roleInstance);
        assertEquals(true, configuration.experimental.distributedTracing.requestIdCompatEnabled);
        assertEquals(true, configuration.experimental.liveMetrics.enabled);
        assertEquals(0, configuration.jmxMetrics.size());
        assertEquals(0, configuration.experimental.instrumentation.size());
        assertEquals(0, configuration.experimental.customInstrumentation.size());
    }
}
