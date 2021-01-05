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
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorActionType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorMatchType;
import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorType;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Moshi.Builder;
import com.squareup.moshi.Types;
import okio.Buffer;
import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static org.junit.Assert.*;

public class ConfigurationTest {

    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    private static Configuration loadConfiguration() throws IOException {
        CharSource json = Resources.asCharSource(Resources.getResource("applicationinsights.json"), Charsets.UTF_8);
        Moshi moshi = MoshiBuilderFactory.createBuilderWithAdaptor();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
        return jsonAdapter.fromJson(json.read());
    }

    @Test
    public void shouldParse() throws IOException {
        Configuration configuration = loadConfiguration();

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", configuration.connectionString);
        assertEquals("Something Good", configuration.role.name);
        assertEquals("xyz123", configuration.role.instance);
        assertEquals(2, configuration.customDimensions.size());
        assertEquals("abc", configuration.customDimensions.get("some key"));
        assertEquals("def", configuration.customDimensions.get("another key"));
        assertEquals(10.0, configuration.sampling.percentage, 0);
        assertEquals(3, configuration.jmxMetrics.size());
        assertEquals("Thread Count", configuration.jmxMetrics.get(0).name);
        assertEquals("java.lang:type=Threading", configuration.jmxMetrics.get(0).objectName);
        assertEquals("ThreadCount", configuration.jmxMetrics.get(0).attribute);
        assertEquals(ImmutableMap.of("level", "error", "enabled", true), configuration.instrumentation.get("logging"));
        assertEquals(60, configuration.heartbeat.intervalSeconds);
        assertEquals("myproxy", configuration.proxy.host);
        assertEquals(8080, configuration.proxy.port);

        assertEquals("debug", configuration.selfDiagnostics.level);
        assertEquals("file", configuration.selfDiagnostics.destination);

        assertEquals("/var/log/applicationinsights/abc.log", configuration.selfDiagnostics.file.path);
        assertEquals(10, configuration.selfDiagnostics.file.maxSizeMb);
        assertEquals(2, configuration.selfDiagnostics.file.maxHistory);
    }

    @Test
    public void shouldParseProcessorConfiguration() throws IOException {

        CharSource json = Resources.asCharSource(Resources.getResource("ApplicationInsights_SpanProcessor.json"), Charsets.UTF_8);
        Moshi moshi = MoshiBuilderFactory.createBuilderWithAdaptor();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class);
        Configuration configuration = jsonAdapter.fromJson(json.read());
        PreviewConfiguration preview = configuration.preview;

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", configuration.connectionString);
        assertEquals(8, preview.processors.size());
        // insert config test
        ProcessorConfig insertConfig = preview.processors.get(0);
        assertEquals("attributes/insert", insertConfig.processorName);
        assertEquals(ProcessorType.attribute, insertConfig.type);
        assertEquals(ProcessorActionType.insert, insertConfig.actions.get(0).action);
        assertEquals("123", insertConfig.actions.get(0).value);
        assertEquals("attribute1", insertConfig.actions.get(0).key);
        assertEquals("anotherKey", insertConfig.actions.get(1).fromAttribute);
        //update config test
        ProcessorConfig updateConfig = preview.processors.get(1);
        assertEquals("attributes/update", updateConfig.processorName);
        assertEquals(ProcessorType.attribute, updateConfig.type);
        assertEquals(ProcessorActionType.update, updateConfig.actions.get(0).action);
        assertEquals("boo", updateConfig.actions.get(0).key);
        assertEquals("foo", updateConfig.actions.get(0).fromAttribute);
        assertEquals("db.secret", updateConfig.actions.get(1).key);
        // selective processing test
        ProcessorConfig selectiveConfig = preview.processors.get(2);
        assertEquals(ProcessorType.attribute, selectiveConfig.type);
        assertEquals("attributes/selectiveProcessing", selectiveConfig.processorName);
        assertEquals(ProcessorMatchType.strict, selectiveConfig.include.matchType);
        assertEquals(2, selectiveConfig.include.spanNames.size());
        assertEquals("svcA", selectiveConfig.include.spanNames.get(0));
        assertEquals(ProcessorMatchType.strict, selectiveConfig.exclude.matchType);
        assertEquals(1, selectiveConfig.exclude.attributes.size());
        assertEquals("redact_trace", selectiveConfig.exclude.attributes.get(0).key);
        assertEquals("false", selectiveConfig.exclude.attributes.get(0).value);
        assertEquals(2, selectiveConfig.actions.size());
        assertEquals("credit_card", selectiveConfig.actions.get(0).key);
        assertEquals(ProcessorActionType.delete, selectiveConfig.actions.get(0).action);
        // log/update name test
        ProcessorConfig logUpdateNameConfig = preview.processors.get(3);
        assertEquals(ProcessorType.log, logUpdateNameConfig.type);
        assertEquals("log/updateName", logUpdateNameConfig.processorName);
        assertEquals(ProcessorMatchType.regexp, logUpdateNameConfig.include.matchType);
        assertEquals(1, logUpdateNameConfig.include.logNames.size());
        assertEquals(".*password.*", logUpdateNameConfig.include.logNames.get(0));
        assertEquals(1, logUpdateNameConfig.name.fromAttributes.size());
        assertEquals("loggerName", logUpdateNameConfig.name.fromAttributes.get(0));
        assertEquals("::", logUpdateNameConfig.name.separator);
        // log/extractAttributes
        ProcessorConfig logExtractAttributesConfig = preview.processors.get(4);
        assertEquals(ProcessorType.log, logExtractAttributesConfig.type);
        assertEquals("log/extractAttributes", logExtractAttributesConfig.processorName);
        assertEquals(1, logExtractAttributesConfig.name.toAttributes.rules.size());
        assertEquals("^/api/v1/document/(?<documentId>.*)/update$", logExtractAttributesConfig.name.toAttributes.rules.get(0));
        // span/update name test
        ProcessorConfig spanUpdateNameConfig = preview.processors.get(5);
        assertEquals(ProcessorType.span, spanUpdateNameConfig.type);
        assertEquals("span/updateName", spanUpdateNameConfig.processorName);
        assertEquals(ProcessorMatchType.regexp, spanUpdateNameConfig.include.matchType);
        assertEquals(1, spanUpdateNameConfig.include.spanNames.size());
        assertEquals(".*password.*", spanUpdateNameConfig.include.spanNames.get(0));
        assertEquals(1, spanUpdateNameConfig.name.fromAttributes.size());
        assertEquals("loggerName", spanUpdateNameConfig.name.fromAttributes.get(0));
        assertEquals("::", spanUpdateNameConfig.name.separator);
        // span/extractAttributes
        ProcessorConfig spanExtractAttributesConfig = preview.processors.get(6);
        assertEquals(ProcessorType.span, spanExtractAttributesConfig.type);
        assertEquals("span/extractAttributes", spanExtractAttributesConfig.processorName);
        assertEquals(1, spanExtractAttributesConfig.name.toAttributes.rules.size());
        assertEquals("^/api/v1/document/(?<documentId>.*)/update$", spanExtractAttributesConfig.name.toAttributes.rules.get(0));
        // attribute/extract
        ProcessorConfig attributesExtractConfig = preview.processors.get(7);
        assertEquals(ProcessorType.attribute, attributesExtractConfig.type);
        assertEquals("attributes/extract", attributesExtractConfig.processorName);
        assertEquals(1, attributesExtractConfig.actions.size());
        assertEquals(ProcessorActionType.extract,attributesExtractConfig.actions.get(0).action);
        assertEquals("http.url",attributesExtractConfig.actions.get(0).key);
        assertEquals(1,attributesExtractConfig.actions.size());
        assertNotNull(attributesExtractConfig.actions.get(0).extractAttribute);
        assertNotNull(attributesExtractConfig.actions.get(0).extractAttribute.extractAttributePattern);
        assertEquals(4,attributesExtractConfig.actions.get(0).extractAttribute.extractAttributeGroupNames.size());
        assertEquals("httpProtocol",attributesExtractConfig.actions.get(0).extractAttribute.extractAttributeGroupNames.get(0));


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
        Moshi moshi = MoshiBuilderFactory.createBasicBuilder();
        Type listOfJmxMetrics = Types.newParameterizedType(List.class, JmxMetric.class);
        JsonReader reader = JsonReader.of(new Buffer().writeUtf8(json));
        reader.setLenient(true);
        JsonAdapter<List<JmxMetric>> jsonAdapter = moshi.adapter(listOfJmxMetrics);
        return jsonAdapter.fromJson(reader);
    }
}
