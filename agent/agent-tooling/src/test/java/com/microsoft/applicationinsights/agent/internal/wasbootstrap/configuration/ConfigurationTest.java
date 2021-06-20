/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.JmxMetric;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.PreviewConfiguration;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorActionType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.internal.authentication.AuthenticationType;
import com.squareup.moshi.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SystemStubsExtension.class)
public class ConfigurationTest {

    @SystemStub
    public EnvironmentVariables envVars = new EnvironmentVariables();

    private static Configuration loadConfiguration() throws IOException {
        return loadConfiguration("applicationinsights.json");
    }

    private static Configuration loadConfiguration(String resourceName) throws IOException {
        CharSource json = Resources.asCharSource(Resources.getResource(resourceName), Charsets.UTF_8);
        Moshi moshi = MoshiBuilderFactory.createBuilderWithAdaptor();
        JsonAdapter<Configuration> jsonAdapter = moshi.adapter(Configuration.class).failOnUnknown();
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
        assertEquals("error", configuration.instrumentation.logging.level);
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
    public void shouldParseFromEnvVar() throws IOException {
        String jmxMetricsJson = "[{" +
                "\"objectName\":\"java.lang:type=ClassLoading\"," +
                "\"attribute\":\"LoadedClassCount\"," +
                "\"display\":\"Loaded Class Count from EnvVar\"}," +
                "{\"objectName\":\"java.lang:type=MemoryPool," +
                "name=Code Cache\",\"attribute\":\"Usage.used\"," +
                "\"display\":\"Code Cache Used from EnvVar\"}]";
        String contentJson = "{\"jmxMetrics\": " + jmxMetricsJson + "," +
                "\"role\":{" +
                "\"name\":\"testrole\"" +
                "}}";
        envVars.set("APPLICATIONINSIGHTS_CONFIGURATION_CONTENT", contentJson);
        envVars.set("APPLICATIONINSIGHTS_CONNECTION_STRING", "InstrumentationKey=11111111-1111-1111-1111-111111111111");

        Configuration configuration = ConfigurationBuilder.create(Paths.get("."), null);

        assertEquals("InstrumentationKey=11111111-1111-1111-1111-111111111111", configuration.connectionString);
        assertEquals("testrole", configuration.role.name);

        List<JmxMetric> jmxMetrics = parseJmxMetricsJson(jmxMetricsJson);
        assertEquals(2, jmxMetrics.size());
        assertEquals(3, configuration.jmxMetrics.size());
        assertEquals(jmxMetrics.get(0).name, configuration.jmxMetrics.get(0).name); // class count is overridden by the env var
        assertEquals(jmxMetrics.get(1).name, configuration.jmxMetrics.get(1).name); // code cache is overridden by the env var
        assertEquals(configuration.jmxMetrics.get(2).name, "Current Thread Count");
    }

    @Test
    public void shouldThrowFromEnvVarIfEmbeddedConnectionString() {
        String contentJson = "{\"connectionString\":\"InstrumentationKey=55555555-5555-5555-5555-555555555555\"," +
                "\"role\":{\"name\":\"testrole\"}}";
        envVars.set("APPLICATIONINSIGHTS_CONFIGURATION_CONTENT", contentJson);

        assertThatThrownBy(() -> ConfigurationBuilder.create(Paths.get("."), null))
                .isInstanceOf(ConfigurationBuilder.ConfigurationException.class);
    }

    @Test
    public void shouldParseProcessorConfiguration() throws IOException {

        Configuration configuration = loadConfiguration("ApplicationInsights_SpanProcessor.json");
        PreviewConfiguration preview = configuration.preview;
        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", configuration.connectionString);
        assertEquals(9, preview.processors.size());
        // insert config test
        ProcessorConfig insertConfig = preview.processors.get(0);
        assertEquals("attributes/insert", insertConfig.id);
        assertEquals(ProcessorType.ATTRIBUTE, insertConfig.type);
        assertEquals(ProcessorActionType.INSERT, insertConfig.actions.get(0).action);
        assertEquals("123", insertConfig.actions.get(0).value);
        assertEquals("attribute1", insertConfig.actions.get(0).key);
        assertEquals("anotherKey", insertConfig.actions.get(1).fromAttribute);
        //update config test
        ProcessorConfig updateConfig = preview.processors.get(1);
        assertEquals("attributes/update", updateConfig.id);
        assertEquals(ProcessorType.ATTRIBUTE, updateConfig.type);
        assertEquals(ProcessorActionType.UPDATE, updateConfig.actions.get(0).action);
        assertEquals("boo", updateConfig.actions.get(0).key);
        assertEquals("foo", updateConfig.actions.get(0).fromAttribute);
        assertEquals("db.secret", updateConfig.actions.get(1).key);
        // selective processing test
        ProcessorConfig selectiveConfig = preview.processors.get(2);
        assertEquals(ProcessorType.ATTRIBUTE, selectiveConfig.type);
        assertEquals("attributes/selectiveProcessing", selectiveConfig.id);
        assertEquals(MatchType.STRICT, selectiveConfig.include.matchType);
        assertEquals(2, selectiveConfig.include.spanNames.size());
        assertEquals("svcA", selectiveConfig.include.spanNames.get(0));
        assertEquals(MatchType.STRICT, selectiveConfig.exclude.matchType);
        assertEquals(1, selectiveConfig.exclude.attributes.size());
        assertEquals("redact_trace", selectiveConfig.exclude.attributes.get(0).key);
        assertEquals("false", selectiveConfig.exclude.attributes.get(0).value);
        assertEquals(2, selectiveConfig.actions.size());
        assertEquals("credit_card", selectiveConfig.actions.get(0).key);
        assertEquals(ProcessorActionType.DELETE, selectiveConfig.actions.get(0).action);
        // log/update name test
        ProcessorConfig logUpdateNameConfig = preview.processors.get(3);
        assertEquals(ProcessorType.LOG, logUpdateNameConfig.type);
        assertEquals("log/updateName", logUpdateNameConfig.id);
        assertEquals(1, logUpdateNameConfig.body.fromAttributes.size());
        assertEquals("loggerName", logUpdateNameConfig.body.fromAttributes.get(0));
        assertEquals("::", logUpdateNameConfig.body.separator);
        // log/extractAttributes
        ProcessorConfig logExtractAttributesConfig = preview.processors.get(4);
        assertEquals(ProcessorType.LOG, logExtractAttributesConfig.type);
        assertEquals("log/extractAttributes", logExtractAttributesConfig.id);
        assertEquals(1, logExtractAttributesConfig.body.toAttributes.rules.size());
        assertEquals("^/api/v1/document/(?<documentId>.*)/update$", logExtractAttributesConfig.body.toAttributes.rules.get(0));
        // span/update name test
        ProcessorConfig spanUpdateNameConfig = preview.processors.get(5);
        assertEquals(ProcessorType.SPAN, spanUpdateNameConfig.type);
        assertEquals("span/updateName", spanUpdateNameConfig.id);
        assertEquals(MatchType.REGEXP, spanUpdateNameConfig.include.matchType);
        assertEquals(1, spanUpdateNameConfig.include.spanNames.size());
        assertEquals(".*password.*", spanUpdateNameConfig.include.spanNames.get(0));
        assertEquals(1, spanUpdateNameConfig.name.fromAttributes.size());
        assertEquals("loggerName", spanUpdateNameConfig.name.fromAttributes.get(0));
        assertEquals("::", spanUpdateNameConfig.name.separator);
        // span/extractAttributes
        ProcessorConfig spanExtractAttributesConfig = preview.processors.get(6);
        assertEquals(ProcessorType.SPAN, spanExtractAttributesConfig.type);
        assertEquals("span/extractAttributes", spanExtractAttributesConfig.id);
        assertEquals(1, spanExtractAttributesConfig.name.toAttributes.rules.size());
        assertEquals("^/api/v1/document/(?<documentId>.*)/update$", spanExtractAttributesConfig.name.toAttributes.rules.get(0));
        // attribute/extract
        ProcessorConfig attributesExtractConfig = preview.processors.get(7);
        assertEquals(ProcessorType.ATTRIBUTE, attributesExtractConfig.type);
        assertEquals("attributes/extract", attributesExtractConfig.id);
        assertEquals(1, attributesExtractConfig.actions.size());
        assertEquals(ProcessorActionType.EXTRACT,attributesExtractConfig.actions.get(0).action);
        assertEquals("http.url",attributesExtractConfig.actions.get(0).key);
        assertEquals(1,attributesExtractConfig.actions.size());
        assertNotNull(attributesExtractConfig.actions.get(0).extractAttribute);
        assertNotNull(attributesExtractConfig.actions.get(0).extractAttribute.pattern);
        assertEquals(4,attributesExtractConfig.actions.get(0).extractAttribute.groupNames.size());
        assertEquals("httpProtocol",attributesExtractConfig.actions.get(0).extractAttribute.groupNames.get(0));
        // metric-filter
        ProcessorConfig metricFilterConfig = preview.processors.get(8);
        assertEquals(ProcessorType.METRIC_FILTER, metricFilterConfig.type);
        assertEquals("metric-filter/exclude-two-metrics", metricFilterConfig.id);
        assertEquals(MatchType.STRICT, metricFilterConfig.exclude.matchType);
        assertEquals(2, metricFilterConfig.exclude.metricNames.size());
        assertEquals("a_test_metric", metricFilterConfig.exclude.metricNames.get(0));
        assertEquals("another_test_metric", metricFilterConfig.exclude.metricNames.get(1));
    }

    @Test
    public void shouldParseAuthenticationConfiguration() throws IOException {

        Configuration configuration = loadConfiguration("applicationinsights_aadauth.json");
        PreviewConfiguration preview = configuration.preview;
        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", configuration.connectionString);
        assertTrue(preview.authentication.enabled);
        assertEquals(AuthenticationType.SAMI, preview.authentication.type);
        assertEquals("123xyz", preview.authentication.clientId);
        assertEquals("tenant123", preview.authentication.tenantId);
        assertEquals("clientsecret123", preview.authentication.clientSecret);
        assertEquals("path/to/keePass", preview.authentication.keePassDatabasePath);
        assertEquals("https://test.com/microsoft/", preview.authentication.authorityHost);
    }

    @Test
    public void shouldUseDefaults() throws IOException {
        envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");
        envVars.set("WEBSITE_INSTANCE_ID", "role instance from website env");

        Configuration configuration = loadConfiguration();

        assertEquals("InstrumentationKey=00000000-0000-0000-0000-000000000000", configuration.connectionString);
        assertEquals("Something Good", configuration.role.name);
        assertEquals("xyz123", configuration.role.instance);
        assertEquals(3, configuration.jmxMetrics.size());
        assertEquals("error", configuration.instrumentation.logging.level);
        assertTrue(configuration.instrumentation.micrometer.enabled);
        assertEquals(60, configuration.heartbeat.intervalSeconds);
    }

    @Test
    public void shouldOverrideConnectionString() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_CONNECTION_STRING", "InstrumentationKey=11111111-1111-1111-1111-111111111111");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("InstrumentationKey=11111111-1111-1111-1111-111111111111", configuration.connectionString);
    }

    @Test
    public void shouldOverrideRoleName() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_ROLE_NAME", "role name from env");
        envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("role name from env", configuration.role.name);
    }

    @Test
    public void shouldOverrideRoleNameWithWebsiteEnvVar() throws IOException {
        envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");

        Configuration configuration = loadConfiguration("applicationinsights_NoRole.json");
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("Role Name From Website Env", configuration.role.name);
    }

    @Test
    public void shouldNotOverrideRoleNameWithWebsiteEnvVar() throws IOException {
        envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("Something Good", configuration.role.name);
    }

    @Test
    public void shouldOverrideRoleNameWithLowercaseWebsiteEnvVarOnAzFn() throws IOException {
        envVars.set("FUNCTIONS_WORKER_RUNTIME", "java");
        envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");

        Configuration configuration = loadConfiguration("applicationinsights_NoRole.json");
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("role name from website env", configuration.role.name);
    }

    @Test
    public void shouldOverrideRoleInstance() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_ROLE_INSTANCE", "role instance from env");
        envVars.set("WEBSITE_INSTANCE_ID", "role instance from website env");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("role instance from env", configuration.role.instance);
    }

    @Test
    public void shouldOverrideRoleInstanceWithWebsiteEnvVar() throws IOException {
        envVars.set("WEBSITE_INSTANCE_ID", "role instance from website env");

        Configuration configuration = loadConfiguration("applicationinsights_NoRole.json");
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("role instance from website env", configuration.role.instance);
    }

    @Test
    public void shouldNotOverrideRoleInstanceWithWebsiteEnvVar() throws IOException {
        envVars.set("WEBSITE_INSTANCE_ID", "role instance from website env");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("xyz123", configuration.role.instance);
    }

    @Test
    public void shouldOverrideSamplingPercentage() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", "0.25");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals(0.25, configuration.sampling.percentage, 0);
    }

    @Test
    public void shouldOverrideLogCaptureThreshold() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL", "TRACE");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("TRACE", configuration.instrumentation.logging.level);
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

    @Test
    public void shouldOverrideSelfDiagnosticsLevel() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL", "DEBUG");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("DEBUG", configuration.selfDiagnostics.level);
    }

    @Test
    public void shouldOverrideSelfDiagnosticsFilePath() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH", "/tmp/ai.log");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertEquals("/tmp/ai.log", configuration.selfDiagnostics.file.path);
    }

    @Test
    public void shouldOverridePreviewOtelApiSupport() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_PREVIEW_OTEL_API_SUPPORT", "true");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertTrue(configuration.preview.openTelemetryApiSupport);
    }

    @Test
    public void shouldOverridePreviewAzureSdkInstrumentation() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_AZURE_SDK_ENABLED", "true");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertTrue(configuration.preview.instrumentation.azureSdk.enabled);
    }

    @Test
    public void shouldOverridePreviewJavaHttpClientInstrumentation() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_JAVA_HTTP_CLIENT_ENABLED", "true");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertTrue(configuration.preview.instrumentation.javaHttpClient.enabled);
    }

    @Test
    public void shouldOverridePreviewJaxwsInstrumentation() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_JAXWS_ENABLED", "true");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertTrue(configuration.preview.instrumentation.jaxws.enabled);
    }

    @Test
    public void shouldOverridePreviewRabbitmqInstrumentation() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_RABBITMQ_ENABLED", "true");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertTrue(configuration.preview.instrumentation.rabbitmq.enabled);
    }

    @Test
    public void shouldOverridePreviewLiveMetricsEnabled() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_PREVIEW_LIVE_METRICS_ENABLED", "false");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertFalse(configuration.preview.liveMetrics.enabled);
    }

    @Test
    public void shouldOverrideInstrumentationCassandraEnabled() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_CASSANDRA_ENABLED", "false");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertFalse(configuration.instrumentation.cassandra.enabled);
    }

    @Test
    public void shouldOverrideInstrumentationJdbcEnabled() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_JDBC_ENABLED", "false");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertFalse(configuration.instrumentation.jdbc.enabled);
    }

    @Test
    public void shouldOverrideInstrumentationJmsEnabled() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_JMS_ENABLED", "false");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertFalse(configuration.instrumentation.jms.enabled);
    }

    @Test
    public void shouldOverrideInstrumentationKafkaEnabled() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_KAFKA_ENABLED", "false");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertFalse(configuration.instrumentation.kafka.enabled);
    }

    @Test
    public void shouldOverrideInstrumentationMicrometerEnabled() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_MICROMETER_ENABLED", "false");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertFalse(configuration.instrumentation.micrometer.enabled);
    }

    @Test
    public void shouldOverrideInstrumentationMongoEnabled() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_MONGO_ENABLED", "false");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertFalse(configuration.instrumentation.mongo.enabled);
    }

    @Test
    public void shouldOverrideInstrumentationRedisEnabled() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_REDIS_ENABLED", "false");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertFalse(configuration.instrumentation.redis.enabled);
    }

    @Test
    public void shouldOverrideInstrumentationSpringSchedulingEnabled() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_SPRING_SCHEDULING_ENABLED", "false");

        Configuration configuration = loadConfiguration();
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertFalse(configuration.instrumentation.springScheduling.enabled);
    }

    @Test
    public void shouldOverrideAadAuthenticationConfig() throws IOException {
        envVars.set("APPLICATIONINSIGHTS_AUTHENTICATION_STRING", "Authorization=AAD;ClientId=12345678");

        Configuration configuration = loadConfiguration("applicationinsights_aadauthenv.json");
        ConfigurationBuilder.overlayEnvVars(configuration);

        assertTrue(configuration.preview.authentication.enabled);
        assertEquals(AuthenticationType.UAMI, configuration.preview.authentication.type);
        assertEquals("12345678", configuration.preview.authentication.clientId);
        assertNull(configuration.preview.authentication.clientSecret);

        envVars.set("APPLICATIONINSIGHTS_AUTHENTICATION_STRING", "Authorization=AAD;ClientId=");

        Configuration configuration2 = loadConfiguration("applicationinsights_aadauthenv.json");
        ConfigurationBuilder.overlayEnvVars(configuration2);

        assertTrue(configuration2.preview.authentication.enabled);
        assertEquals(AuthenticationType.SAMI, configuration2.preview.authentication.type);
        assertNull(configuration2.preview.authentication.clientId);
        assertNull(configuration2.preview.authentication.clientSecret);
    }

    @Test
    public void shouldUseRpConfigRole() {
        Configuration configuration = new Configuration();
        RpConfiguration rpConfiguration = new RpConfiguration();
        rpConfiguration.role.name = "role-name-from-rp";
        rpConfiguration.role.instance = "role-instance-from-rp";
        ConfigurationBuilder.overlayRpConfiguration(configuration, rpConfiguration);

        assertEquals("role-name-from-rp", configuration.role.name);
        assertEquals("role-instance-from-rp", configuration.role.instance);
    }

    @Test
    public void shouldNotUseRpConfigRole() {
        Configuration configuration = new Configuration();
        configuration.role.name = "role-name";
        configuration.role.instance = "role-instance";
        RpConfiguration rpConfiguration = new RpConfiguration();
        rpConfiguration.role.name = "role-name-from-rp";
        rpConfiguration.role.instance = "role-instance-from-rp";
        ConfigurationBuilder.overlayRpConfiguration(configuration, rpConfiguration);

        assertEquals("role-name", configuration.role.name);
        assertEquals("role-instance", configuration.role.instance);
    }

    @Test
    public void shouldNotParseFaultyJson() {
        assertThatThrownBy(() -> loadConfiguration("applicationinsights_faulty.json"))
                .isInstanceOf(JsonDataException.class);
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
