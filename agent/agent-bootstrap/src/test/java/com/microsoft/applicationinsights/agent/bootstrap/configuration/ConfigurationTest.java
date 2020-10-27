package com.microsoft.applicationinsights.agent.bootstrap.configuration;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.JmxMetric;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.PreviewConfiguration;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okio.Buffer;
import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static org.junit.Assert.*;

public class ConfigurationTest {

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    @Test
    public void shouldParse() throws IOException {
        Configuration configuration = loadConfiguration();

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", configuration.connectionString);
        assertEquals("Something Good", configuration.role.name);
        assertEquals("xyz123", configuration.role.instance);
        assertEquals(2, configuration.customDimensions.size());
        assertEquals("abc", configuration.customDimensions.get("some key"));
        assertEquals("def", configuration.customDimensions.get("another key"));
        assertEquals((Double) 10.0, configuration.sampling.percentage);
        assertEquals(3, configuration.jmxMetrics.size());
        assertEquals("Thread Count", configuration.jmxMetrics.get(0).name);
        assertEquals("java.lang:type=Threading", configuration.jmxMetrics.get(0).objectName);
        assertEquals("ThreadCount", configuration.jmxMetrics.get(0).attribute);
        assertEquals(ImmutableMap.of("level", "error", "enabled", true), configuration.instrumentation.get("logging"));
        assertEquals(60, configuration.heartbeat.intervalSeconds);
        assertEquals("myproxy", configuration.proxy.host);
        assertEquals(8080, configuration.proxy.port);

        PreviewConfiguration preview = configuration.preview;

        assertEquals("file", preview.selfDiagnostics.destination);
        assertEquals("/var/log/applicationinsights", preview.selfDiagnostics.directory);
        assertEquals("error", preview.selfDiagnostics.level);
        assertEquals(10, preview.selfDiagnostics.maxSizeMB);
    }

    @Test
    public void shouldUseDefaults() throws IOException {
        Configuration configuration = loadConfiguration();

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", configuration.connectionString);
        assertEquals("Something Good", configuration.role.name);
        assertEquals("xyz123", configuration.role.instance);
        assertEquals(3, configuration.jmxMetrics.size());
        assertEquals("error", configuration.instrumentation.get("logging").get("level"));
        assertEquals(true, configuration.instrumentation.get("micrometer").get("enabled"));
        assertEquals(true, configuration.instrumentation.get("logging").get("enabled"));
        assertEquals(60, configuration.heartbeat.intervalSeconds);
    }

    @Test
    public void shouldOverrideSamplingPercentage() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", "0.25");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertTrue(configuration.sampling.percentage == 0.25);
    }

    @Test
    public void shouldOverrideLogCaptureThreshold() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL", "TRACE");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("TRACE", configuration.instrumentation.get("logging").get("level"));
        assertEquals(true, configuration.instrumentation.get("logging").get("enabled"));
    }

    @Test
    public void shouldOverrideJmxMetrics() throws IOException {
        String jmxMetricsJson = "[{'objectName': 'java.lang:type=ClassLoading','attribute': 'LoadedClassCount','display': 'Loaded Class Count from EnvVar'}," +
                "{'objectName': 'java.lang:type=MemoryPool,name=Code Cache','attribute': 'Usage.used','display': 'Code Cache Used from EnvVar'}]";
        envVars.set("APPLICATIONINSIGHTS_JMX_METRICS", jmxMetricsJson);

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        List<JmxMetric> jmxMetrics = parseJmxMetricsJson(jmxMetricsJson);
        assertEquals(2, jmxMetrics.size());
        assertEquals(3, configuration.jmxMetrics.size());
        assertEquals(jmxMetrics.get(0).name, configuration.jmxMetrics.get(0).name); // class count is overridden by the env var
        assertEquals(jmxMetrics.get(1).name, configuration.jmxMetrics.get(1).name); // code cache is overridden by the env var
        assertEquals(configuration.jmxMetrics.get(2).name, "Current Thread Count");
    }

    private List<JmxMetric> parseJmxMetricsJson(String json) throws IOException {
        Moshi moshi = new Moshi.Builder().build();
        Type listOfJmxMetrics = Types.newParameterizedType(List.class, JmxMetric.class);
        JsonReader reader = JsonReader.of(new Buffer().writeUtf8(json));
        reader.setLenient(true);
        JsonAdapter<List<JmxMetric>> jsonAdapter = moshi.adapter(listOfJmxMetrics);
        return jsonAdapter.fromJson(reader);
    }

    private static Configuration loadConfiguration() throws IOException {
        CharSource json = Resources.asCharSource(Resources.getResource("ApplicationInsights.json"), Charsets.UTF_8);
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
        return jsonAdapter.fromJson(json.read());
    }
}
