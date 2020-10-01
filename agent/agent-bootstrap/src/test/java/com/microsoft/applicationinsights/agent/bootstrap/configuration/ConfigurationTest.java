package com.microsoft.applicationinsights.agent.bootstrap.configuration;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.JmxMetric;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.PreviewConfiguration;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.SpanProcessorConfig;
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
        InstrumentationSettings instrumentationSettings = configuration.instrumentationSettings;
        PreviewConfiguration preview = instrumentationSettings.preview;

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", instrumentationSettings.connectionString);
        assertEquals("Something Good", preview.roleName);
        assertEquals("xyz123", preview.roleInstance);
        assertEquals((Double) 10.0, preview.sampling.fixedRate.percentage);
        assertEquals(60, preview.heartbeat.intervalSeconds);
        assertEquals(3, preview.jmxMetrics.size());
        assertEquals("java.lang:type=Threading", preview.jmxMetrics.get(0).objectName);
        assertEquals("ThreadCount", preview.jmxMetrics.get(0).attribute);
        assertEquals("Thread Count", preview.jmxMetrics.get(0).display);
        assertEquals(ImmutableMap.of("threshold", "error", "enabled", "true"), preview.instrumentation.get("logging"));
        assertEquals("myproxy", preview.httpProxy.host);
        assertEquals(8080, preview.httpProxy.port);

        assertEquals("file", preview.selfDiagnostics.destination);
        assertEquals("/var/log/applicationinsights", preview.selfDiagnostics.directory);
        assertEquals("error", preview.selfDiagnostics.level);
        assertEquals(10, preview.selfDiagnostics.maxSizeMB);
    }

    @Test
    public void shouldParseSpanProcessorConfiguration() throws IOException {

        CharSource json = Resources.asCharSource(Resources.getResource("ApplicationInsights_SpanProcessor.json"), Charsets.UTF_8);
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
        Configuration configuration = jsonAdapter.fromJson(json.read());

        InstrumentationSettings instrumentationSettings = configuration.instrumentationSettings;
        PreviewConfiguration preview = instrumentationSettings.preview;

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", instrumentationSettings.connectionString);
        assertEquals(3,preview.spanprocessors.keySet().size());
        // insert config test
        SpanProcessorConfig insertConfig=preview.spanprocessors.get("attributes/insert");
        assertEquals("insert", insertConfig.insertActions.get(0).action);
        assertEquals("123", insertConfig.insertActions.get(0).value);
        assertEquals("attribute1", insertConfig.insertActions.get(0).key);
        assertEquals("anotherkey", insertConfig.insertActions.get(1).from_attribute);
        //update config test
        SpanProcessorConfig updateConfig=preview.spanprocessors.get("attributes/update");
        assertEquals("update", updateConfig.otherActions.get(0).action);
        assertEquals("boo", updateConfig.otherActions.get(0).key);
        assertEquals("foo", updateConfig.otherActions.get(0).from_attribute);
        assertEquals("db.secret", updateConfig.otherActions.get(1).key);
        // selective processing test
        SpanProcessorConfig selectiveConfig=preview.spanprocessors.get("attributes/selectiveprocessing");
        assertEquals("strict", selectiveConfig.include.match_type);
        assertEquals(2, selectiveConfig.include.span_names.size());
        assertEquals("svcA", selectiveConfig.include.span_names.get(0));
        assertEquals("strict", selectiveConfig.exclude.match_type);
        assertEquals(1, selectiveConfig.exclude.attributes.size());
        assertEquals("redact_trace", selectiveConfig.exclude.attributes.get(0).key);
        assertEquals("false", selectiveConfig.exclude.attributes.get(0).value);
        assertEquals(2, selectiveConfig.otherActions.size());
        assertEquals("credit_card", selectiveConfig.otherActions.get(0).key);
        assertEquals("delete", selectiveConfig.otherActions.get(0).action);


    }



    @Test
    public void shouldUseDefaults() throws IOException {
        Configuration configuration = loadConfiguration();
        InstrumentationSettings instrumentationSettings = configuration.instrumentationSettings;
        PreviewConfiguration preview = instrumentationSettings.preview;

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", instrumentationSettings.connectionString);
        assertEquals("Something Good", preview.roleName);
        assertEquals("xyz123", preview.roleInstance);
        assertEquals(60, preview.heartbeat.intervalSeconds);
        assertEquals(3, preview.jmxMetrics.size());
        assertEquals("error", preview.instrumentation.get("logging").get("threshold"));
        assertEquals("true", preview.instrumentation.get("micrometer").get("enabled"));
        assertEquals("true", preview.instrumentation.get("logging").get("enabled"));
    }

    @Test
    public void shouldOverrideSamplingRate() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", "25");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertTrue(configuration.instrumentationSettings.preview.sampling.fixedRate.percentage == 25);
    }

    @Test
    public void shouldOverrideLogCaptureThreshold() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_LOGGING_THRESHOLD", "TRACE");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("TRACE", configuration.instrumentationSettings.preview.instrumentation.get("logging").get("threshold"));
        assertEquals("true", configuration.instrumentationSettings.preview.instrumentation.get("logging").get("enabled"));
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
        assertEquals(3, configuration.instrumentationSettings.preview.jmxMetrics.size());
        assertEquals(jmxMetrics.get(0).display, configuration.instrumentationSettings.preview.jmxMetrics.get(0).display); // class count is overridden by the env var
        assertEquals(jmxMetrics.get(1).display, configuration.instrumentationSettings.preview.jmxMetrics.get(1).display); // code cache is overridden by the env var
        assertEquals(configuration.instrumentationSettings.preview.jmxMetrics.get(2).display, "Current Thread Count");
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
