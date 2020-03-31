package com.microsoft.applicationinsights.agent.bootstrap.configuration;

import java.io.IOException;
import java.util.Arrays;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.PreviewConfiguration;
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

        InstrumentationSettings instrumentationSettings = configuration.instrumentationSettings;
        PreviewConfiguration preview = instrumentationSettings.preview;

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", instrumentationSettings.connectionString);
        assertEquals("Something Good", preview.roleName);
        assertEquals("xyz123", preview.roleInstance);
        assertEquals((Double) 10.0, preview.sampling.fixedRate.percentage);
        assertEquals(false, preview.liveMetrics.enabled);
        assertEquals(3, preview.jmxMetrics.size());
        assertEquals("java.lang:type=Threading", preview.jmxMetrics.get(0).objectName);
        assertEquals("ThreadCount", preview.jmxMetrics.get(0).attribute);
        assertEquals("Thread Count", preview.jmxMetrics.get(0).display);
        assertEquals(ImmutableMap.of("__comment",
                Arrays.asList("this sets the explain plan threshold ...", "this is a multi-line comment"),
                "explainPlanThresholdInMS", 20000.0), preview.instrumentation.get("jdbc"));
        assertEquals(ImmutableMap.of("enabled", false), preview.instrumentation.get("logging"));
    }

    @Test
    public void shouldUseDefaults() throws IOException {

        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
        Configuration configuration = jsonAdapter.fromJson("{}");

        InstrumentationSettings instrumentationSettings = configuration.instrumentationSettings;
        PreviewConfiguration preview = instrumentationSettings.preview;

        assertEquals(null, instrumentationSettings.connectionString);
        assertEquals(null, preview.roleName);
        assertEquals(null, preview.roleInstance);
        assertEquals(true, preview.liveMetrics.enabled);
        assertEquals(0, preview.jmxMetrics.size());
        assertEquals(0, preview.instrumentation.size());
    }
}
