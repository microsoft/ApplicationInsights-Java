package com.microsoft.applicationinsights.agentc.internal;

import java.io.IOException;
import java.util.Arrays;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
        assertEquals(true, configuration.distributedTracing.w3cEnabled);
        assertEquals(false, configuration.distributedTracing.w3cBackCompatEnabled);
        assertEquals(false, configuration.liveMetrics.enabled);
        assertEquals(ImmutableMap.of("k8s.pod.name", "amazing-product", "k8s.pod.namespace", "product-ns"),
                configuration.telemetryContext);
        assertEquals(3, configuration.jmxMetrics.size());
        assertEquals("java.lang:type=Threading", configuration.jmxMetrics.get(0).objectName);
        assertEquals("ThreadCount", configuration.jmxMetrics.get(0).attribute);
        assertEquals("Thread Count", configuration.jmxMetrics.get(0).display);
        assertEquals(ImmutableMap.of("__comment",
                Arrays.asList("this sets the explain plan threshold ...", "this is a multi-line comment"),
                "explainPlanThresholdInMS", 20000.0), configuration.instrumentation.get("jdbc"));
        assertEquals(ImmutableMap.of("enabled", false), configuration.instrumentation.get("logging"));
        assertEquals(2, configuration.customInstrumentation.size());
        assertEquals("com.example.MyObject", configuration.customInstrumentation.get(0).className);
        assertEquals("doSomething", configuration.customInstrumentation.get(0).methodName);
    }

    @Test
    public void shouldUseDefaults() {

        Configuration configuration = new Gson().fromJson("{}", Configuration.class);

        assertEquals(null, configuration.connectionString);
        assertEquals(null, configuration.roleName);
        assertEquals(null, configuration.roleInstance);
        assertEquals(false, configuration.distributedTracing.w3cEnabled);
        assertEquals(true, configuration.distributedTracing.w3cBackCompatEnabled);
        assertEquals(true, configuration.liveMetrics.enabled);
        assertEquals(0, configuration.telemetryContext.size());
        assertEquals(0, configuration.jmxMetrics.size());
        assertEquals(0, configuration.instrumentation.size());
        assertEquals(0, configuration.customInstrumentation.size());
    }
}
