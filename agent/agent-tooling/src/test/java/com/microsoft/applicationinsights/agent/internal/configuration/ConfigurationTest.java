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

package com.microsoft.applicationinsights.agent.internal.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.JmxMetric;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.MatchType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.PreviewConfiguration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorActionType;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorType;
import io.opentelemetry.api.common.AttributeKey;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

@ExtendWith(SystemStubsExtension.class)
class ConfigurationTest {

  @SystemStub EnvironmentVariables envVars = new EnvironmentVariables();
  @SystemStub SystemProperties systemProperties = new SystemProperties();

  @Test
  void shouldParse() throws IOException {
    Configuration configuration = loadConfiguration();

    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
    assertThat(configuration.role.name).isEqualTo("Something Good");
    assertThat(configuration.role.instance).isEqualTo("xyz123");
    assertThat(configuration.customDimensions.size()).isEqualTo(2);
    assertThat(configuration.customDimensions.get("some key")).isEqualTo("abc");
    assertThat(configuration.customDimensions.get("another key")).isEqualTo("def");
    assertThat(configuration.sampling.percentage).isEqualTo(10.0f);
    assertThat(configuration.jmxMetrics.size()).isEqualTo(3);
    assertThat(configuration.jmxMetrics.get(0).name).isEqualTo("Thread Count");
    assertThat(configuration.jmxMetrics.get(0).objectName).isEqualTo("java.lang:type=Threading");
    assertThat(configuration.jmxMetrics.get(0).attribute).isEqualTo("ThreadCount");
    assertThat(configuration.instrumentation.logging.level).isEqualTo("error");
    assertThat(configuration.heartbeat.intervalSeconds).isEqualTo(60);
    assertThat(configuration.proxy.host).isEqualTo("myproxy");
    assertThat(configuration.proxy.port).isEqualTo(8080);

    assertThat(configuration.selfDiagnostics.level).isEqualTo("debug");
    assertThat(configuration.selfDiagnostics.destination).isEqualTo("file");

    assertThat(configuration.selfDiagnostics.file.path)
        .isEqualTo("/var/log/applicationinsights/abc.log");
    assertThat(configuration.selfDiagnostics.file.maxSizeMb).isEqualTo(10);
    assertThat(configuration.selfDiagnostics.file.maxHistory).isEqualTo(2);
  }

  @Test
  void shouldParseFromEnvVar() throws IOException {
    String jmxMetricsJson =
        "[{"
            + "\"objectName\":\"java.lang:type=ClassLoading\","
            + "\"attribute\":\"LoadedClassCount\","
            + "\"display\":\"Loaded Class Count from EnvVar\"},"
            + "{\"objectName\":\"java.lang:type=MemoryPool,"
            + "name=Code Cache\",\"attribute\":\"Usage.used\","
            + "\"display\":\"Code Cache Used from EnvVar\"}]";
    String contentJson =
        "{\"jmxMetrics\": " + jmxMetricsJson + "," + "\"role\":{" + "\"name\":\"testrole\"" + "}}";
    envVars.set("APPLICATIONINSIGHTS_CONFIGURATION_CONTENT", contentJson);
    envVars.set(
        "APPLICATIONINSIGHTS_CONNECTION_STRING",
        "InstrumentationKey=11111111-1111-1111-1111-111111111111");

    Configuration configuration = ConfigurationBuilder.create(Paths.get("."), null);

    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=11111111-1111-1111-1111-111111111111");
    assertThat(configuration.role.name).isEqualTo("testrole");

    List<JmxMetric> jmxMetrics = parseJmxMetricsJson(jmxMetricsJson);
    assertThat(jmxMetrics.size()).isEqualTo(2);
    assertThat(configuration.jmxMetrics.size()).isEqualTo(3);
    assertThat(configuration.jmxMetrics.get(0).name)
        .isEqualTo(jmxMetrics.get(0).name); // class count is overridden by the env var
    assertThat(configuration.jmxMetrics.get(1).name)
        .isEqualTo(jmxMetrics.get(1).name); // code cache is overridden by the env var
    assertThat("Current Thread Count").isEqualTo(configuration.jmxMetrics.get(2).name);
  }

  @Test
  void shouldThrowFromEnvVarIfEmbeddedConnectionString() {
    String contentJson =
        "{\"connectionString\":\"InstrumentationKey=55555555-5555-5555-5555-555555555555\","
            + "\"role\":{\"name\":\"testrole\"}}";
    envVars.set("APPLICATIONINSIGHTS_CONFIGURATION_CONTENT", contentJson);

    assertThatThrownBy(() -> ConfigurationBuilder.create(Paths.get("."), null))
        .isInstanceOf(ConfigurationBuilder.ConfigurationException.class);
  }

  @Test
  void shouldParseProcessorConfiguration() throws IOException {

    Configuration configuration = loadConfiguration("ApplicationInsights_SpanProcessor.json");
    PreviewConfiguration preview = configuration.preview;
    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
    assertThat(preview.processors.size()).isEqualTo(9);
    // insert config test
    ProcessorConfig insertConfig = preview.processors.get(0);
    assertThat(insertConfig.id).isEqualTo("attributes/insert");
    assertThat(insertConfig.type).isEqualTo(ProcessorType.ATTRIBUTE);
    assertThat(insertConfig.actions.get(0).action).isEqualTo(ProcessorActionType.INSERT);
    assertThat(insertConfig.actions.get(0).value).isEqualTo("123");
    assertThat(insertConfig.actions.get(0).key).isEqualTo(AttributeKey.stringKey("attribute1"));
    assertThat(insertConfig.actions.get(1).fromAttribute)
        .isEqualTo(AttributeKey.stringKey("anotherKey"));
    // update config test
    ProcessorConfig updateConfig = preview.processors.get(1);
    assertThat(updateConfig.id).isEqualTo("attributes/update");
    assertThat(updateConfig.type).isEqualTo(ProcessorType.ATTRIBUTE);
    assertThat(updateConfig.actions.get(0).action).isEqualTo(ProcessorActionType.UPDATE);
    assertThat(updateConfig.actions.get(0).key).isEqualTo(AttributeKey.stringKey("boo"));
    assertThat(updateConfig.actions.get(0).fromAttribute).isEqualTo(AttributeKey.stringKey("foo"));
    assertThat(updateConfig.actions.get(1).key).isEqualTo(AttributeKey.stringKey("db.secret"));
    // selective processing test
    ProcessorConfig selectiveConfig = preview.processors.get(2);
    assertThat(selectiveConfig.type).isEqualTo(ProcessorType.ATTRIBUTE);
    assertThat(selectiveConfig.id).isEqualTo("attributes/selectiveProcessing");
    assertThat(selectiveConfig.include.matchType).isEqualTo(MatchType.STRICT);
    assertThat(selectiveConfig.include.spanNames.size()).isEqualTo(2);
    assertThat(selectiveConfig.include.spanNames.get(0)).isEqualTo("svcA");
    assertThat(selectiveConfig.exclude.matchType).isEqualTo(MatchType.STRICT);
    assertThat(selectiveConfig.exclude.attributes.size()).isEqualTo(1);
    assertThat(selectiveConfig.exclude.attributes.get(0).key).isEqualTo("redact_trace");
    assertThat(selectiveConfig.exclude.attributes.get(0).value).isEqualTo("false");
    assertThat(selectiveConfig.actions.size()).isEqualTo(2);
    assertThat(selectiveConfig.actions.get(0).key).isEqualTo(AttributeKey.stringKey("credit_card"));
    assertThat(selectiveConfig.actions.get(0).action).isEqualTo(ProcessorActionType.DELETE);
    // log/update name test
    ProcessorConfig logUpdateNameConfig = preview.processors.get(3);
    assertThat(logUpdateNameConfig.type).isEqualTo(ProcessorType.LOG);
    assertThat(logUpdateNameConfig.id).isEqualTo("log/updateName");
    assertThat(logUpdateNameConfig.body.fromAttributes.size()).isEqualTo(1);
    assertThat(logUpdateNameConfig.body.fromAttributes.get(0)).isEqualTo("loggerName");
    assertThat(logUpdateNameConfig.body.separator).isEqualTo("::");
    // log/extractAttributes
    ProcessorConfig logExtractAttributesConfig = preview.processors.get(4);
    assertThat(logExtractAttributesConfig.type).isEqualTo(ProcessorType.LOG);
    assertThat(logExtractAttributesConfig.id).isEqualTo("log/extractAttributes");
    assertThat(logExtractAttributesConfig.body.toAttributes.rules.size()).isEqualTo(1);
    assertThat(logExtractAttributesConfig.body.toAttributes.rules.get(0))
        .isEqualTo("^/api/v1/document/(?<documentId>.*)/update$");
    // span/update name test
    ProcessorConfig spanUpdateNameConfig = preview.processors.get(5);
    assertThat(spanUpdateNameConfig.type).isEqualTo(ProcessorType.SPAN);
    assertThat(spanUpdateNameConfig.id).isEqualTo("span/updateName");
    assertThat(spanUpdateNameConfig.include.matchType).isEqualTo(MatchType.REGEXP);
    assertThat(spanUpdateNameConfig.include.spanNames.size()).isEqualTo(1);
    assertThat(spanUpdateNameConfig.include.spanNames.get(0)).isEqualTo(".*password.*");
    assertThat(spanUpdateNameConfig.name.fromAttributes.size()).isEqualTo(1);
    assertThat(spanUpdateNameConfig.name.fromAttributes.get(0)).isEqualTo("loggerName");
    assertThat(spanUpdateNameConfig.name.separator).isEqualTo("::");
    // span/extractAttributes
    ProcessorConfig spanExtractAttributesConfig = preview.processors.get(6);
    assertThat(spanExtractAttributesConfig.type).isEqualTo(ProcessorType.SPAN);
    assertThat(spanExtractAttributesConfig.id).isEqualTo("span/extractAttributes");
    assertThat(spanExtractAttributesConfig.name.toAttributes.rules.size()).isEqualTo(1);
    assertThat(spanExtractAttributesConfig.name.toAttributes.rules.get(0))
        .isEqualTo("^/api/v1/document/(?<documentId>.*)/update$");
    // attribute/extract
    ProcessorConfig attributesExtractConfig = preview.processors.get(7);
    assertThat(attributesExtractConfig.type).isEqualTo(ProcessorType.ATTRIBUTE);
    assertThat(attributesExtractConfig.id).isEqualTo("attributes/extract");
    assertThat(attributesExtractConfig.actions.size()).isEqualTo(1);
    assertThat(attributesExtractConfig.actions.get(0).action)
        .isEqualTo(ProcessorActionType.EXTRACT);
    assertThat(attributesExtractConfig.actions.get(0).key)
        .isEqualTo(AttributeKey.stringKey("http.url"));
    assertThat(attributesExtractConfig.actions.size()).isEqualTo(1);
    assertThat(attributesExtractConfig.actions.get(0).extractAttribute).isNotNull();
    assertThat(attributesExtractConfig.actions.get(0).extractAttribute.pattern).isNotNull();
    assertThat(attributesExtractConfig.actions.get(0).extractAttribute.groupNames.size())
        .isEqualTo(4);
    assertThat(attributesExtractConfig.actions.get(0).extractAttribute.groupNames.get(0))
        .isEqualTo("httpProtocol");
    // metric-filter
    ProcessorConfig metricFilterConfig = preview.processors.get(8);
    assertThat(metricFilterConfig.type).isEqualTo(ProcessorType.METRIC_FILTER);
    assertThat(metricFilterConfig.id).isEqualTo("metric-filter/exclude-two-metrics");
    assertThat(metricFilterConfig.exclude.matchType).isEqualTo(MatchType.STRICT);
    assertThat(metricFilterConfig.exclude.metricNames.size()).isEqualTo(2);
    assertThat(metricFilterConfig.exclude.metricNames.get(0)).isEqualTo("a_test_metric");
    assertThat(metricFilterConfig.exclude.metricNames.get(1)).isEqualTo("another_test_metric");
  }

  @Test
  void shouldParseAuthenticationConfiguration() throws IOException {

    Configuration configuration = loadConfiguration("applicationinsights_aadauth.json");
    PreviewConfiguration preview = configuration.preview;
    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
    assertThat(preview.authentication.enabled).isTrue();
    assertThat(preview.authentication.type).isEqualTo(Configuration.AuthenticationType.SAMI);
    assertThat(preview.authentication.clientId).isEqualTo("123xyz");
    assertThat(preview.authentication.tenantId).isEqualTo("tenant123");
    assertThat(preview.authentication.clientSecret).isEqualTo("clientsecret123");
    assertThat(preview.authentication.authorityHost).isEqualTo("https://test.com/microsoft/");
  }

  @Test
  void shouldUseDefaults() throws IOException {
    envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");
    envVars.set("WEBSITE_INSTANCE_ID", "role instance from website env");

    Configuration configuration = loadConfiguration();

    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=00000000-0000-0000-0000-000000000000");
    assertThat(configuration.role.name).isEqualTo("Something Good");
    assertThat(configuration.role.instance).isEqualTo("xyz123");
    assertThat(configuration.jmxMetrics.size()).isEqualTo(3);
    assertThat(configuration.instrumentation.logging.level).isEqualTo("error");
    assertThat(configuration.instrumentation.micrometer.enabled).isTrue();
    assertThat(configuration.heartbeat.intervalSeconds).isEqualTo(60);
  }

  @Test
  void shouldOverrideConnectionStringWithEnvVar() throws IOException {
    envVars.set(
        "APPLICATIONINSIGHTS_CONNECTION_STRING",
        "InstrumentationKey=11111111-1111-1111-1111-111111111111");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=11111111-1111-1111-1111-111111111111");
  }

  @Test
  void shouldOverrideConnectionStringWithSysProp() throws IOException {
    systemProperties.set(
        "applicationinsights.connection.string",
        "InstrumentationKey=11111111-1111-1111-1111-111111111111");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=11111111-1111-1111-1111-111111111111");
  }

  @Test
  void shouldOverrideConnectionStringWithBothEnvVarAndSysProp() throws IOException {
    envVars.set(
        "APPLICATIONINSIGHTS_CONNECTION_STRING",
        "InstrumentationKey=11111111-1111-1111-1111-111111111111");
    systemProperties.set(
        "applicationinsights.connection.string",
        "InstrumentationKey=22222222-2222-2222-2222-222222222222");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.connectionString)
        .isEqualTo("InstrumentationKey=22222222-2222-2222-2222-222222222222");
  }

  @Test
  void shouldOverrideRoleNameWithEnvVar() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_ROLE_NAME", "role name from env");

    envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.name).isEqualTo("role name from env");
  }

  @Test
  void shouldOverrideRoleNameWithSysProp() throws IOException {
    systemProperties.set("applicationinsights.role.name", "role name from sys");

    envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.name).isEqualTo("role name from sys");
  }

  @Test
  void shouldOverrideRoleNameWithBothEnvVarAndSysProp() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_ROLE_NAME", "role name from env");
    systemProperties.set("applicationinsights.role.name", "role name from sys");

    envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.name).isEqualTo("role name from sys");
  }

  @Test
  void shouldOverrideRoleNameWithWebsiteEnvVar() throws IOException {
    envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");

    Configuration configuration = loadConfiguration("applicationinsights_NoRole.json");
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.name).isEqualTo("Role Name From Website Env");
  }

  @Test
  void shouldNotOverrideRoleNameWithWebsiteEnvVar() throws IOException {
    envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.name).isEqualTo("Something Good");
  }

  @Test
  void shouldOverrideRoleNameWithLowercaseWebsiteEnvVarOnAzFn() throws IOException {
    envVars.set("FUNCTIONS_WORKER_RUNTIME", "java");
    envVars.set("WEBSITE_SITE_NAME", "Role Name From Website Env");

    Configuration configuration = loadConfiguration("applicationinsights_NoRole.json");
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.name).isEqualTo("role name from website env");
  }

  @Test
  void shouldOverrideRoleInstanceWithEnvVar() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_ROLE_INSTANCE", "role instance from env");

    envVars.set("WEBSITE_INSTANCE_ID", "role instance from website env");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.instance).isEqualTo("role instance from env");
  }

  @Test
  void shouldOverrideRoleInstanceWithSysProp() throws IOException {
    systemProperties.set("applicationinsights.role.instance", "role instance from sys");

    envVars.set("WEBSITE_INSTANCE_ID", "role instance from website env");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.instance).isEqualTo("role instance from sys");
  }

  @Test
  void shouldOverrideRoleInstanceWithBothEnvVarAndSysProp() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_ROLE_INSTANCE", "role instance from env");
    systemProperties.set("applicationinsights.role.instance", "role instance from sys");

    envVars.set("WEBSITE_INSTANCE_ID", "role instance from website env");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.instance).isEqualTo("role instance from sys");
  }

  @Test
  void shouldOverrideRoleInstanceWithWebsiteEnvVar() throws IOException {
    envVars.set("WEBSITE_INSTANCE_ID", "role instance from website env");

    Configuration configuration = loadConfiguration("applicationinsights_NoRole.json");
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.instance).isEqualTo("role instance from website env");
  }

  @Test
  void shouldNotOverrideRoleInstanceWithWebsiteEnvVar() throws IOException {
    envVars.set("WEBSITE_INSTANCE_ID", "role instance from website env");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.role.instance).isEqualTo("xyz123");
  }

  @Test
  void shouldOverrideSamplingPercentage() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE", "0.25");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.sampling.percentage).isEqualTo(0.25f);
  }

  @Test
  void shouldOverrideLogCaptureThreshold() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL", "TRACE");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.instrumentation.logging.level).isEqualTo("TRACE");
  }

  @Test
  void shouldOverrideJmxMetrics() throws IOException {
    String jmxMetricsJson =
        "[{\"objectName\": \"java.lang:type=ClassLoading\",\"attribute\": \"LoadedClassCount\",\"display\": \"Loaded Class Count from EnvVar\"},"
            + "{\"objectName\": \"java.lang:type=MemoryPool,name=Code Cache\",\"attribute\": \"Usage.used\",\"display\": \"Code Cache Used from EnvVar\"}]";
    envVars.set("APPLICATIONINSIGHTS_JMX_METRICS", jmxMetricsJson);

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    List<JmxMetric> jmxMetrics = parseJmxMetricsJson(jmxMetricsJson);
    assertThat(jmxMetrics.size()).isEqualTo(2);
    assertThat(configuration.jmxMetrics.size()).isEqualTo(3);
    assertThat(configuration.jmxMetrics.get(0).name)
        .isEqualTo(jmxMetrics.get(0).name); // class count is overridden by the env var
    assertThat(configuration.jmxMetrics.get(1).name)
        .isEqualTo(jmxMetrics.get(1).name); // code cache is overridden by the env var
    assertThat("Current Thread Count").isEqualTo(configuration.jmxMetrics.get(2).name);
  }

  @Test
  void shouldOverrideSelfDiagnosticsLevel() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL", "DEBUG");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.selfDiagnostics.level).isEqualTo("DEBUG");
  }

  @Test
  void shouldOverrideSelfDiagnosticsFilePath() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH", "/tmp/ai.log");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.selfDiagnostics.file.path).isEqualTo("/tmp/ai.log");
  }

  @Test
  void shouldOverridePreviewOtelApiSupport() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_PREVIEW_OTEL_API_SUPPORT", "true");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.preview.openTelemetryApiSupport).isTrue();
  }

  @Test
  void shouldOverridePreviewAzureSdkInstrumentation() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_AZURE_SDK_ENABLED", "true");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.preview.instrumentation.azureSdk.enabled).isTrue();
  }

  @Test
  void shouldOverridePreviewSpringIntegrationInstrumentation() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_SPRING_INTEGRATION_ENABLED", "true");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.preview.instrumentation.springIntegration.enabled).isTrue();
  }

  @Test
  void shouldOverridePreviewLiveMetricsEnabled() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_PREVIEW_LIVE_METRICS_ENABLED", "false");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.preview.liveMetrics.enabled).isFalse();
  }

  @Test
  void shouldOverrideInstrumentationCassandraEnabled() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_CASSANDRA_ENABLED", "false");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.instrumentation.cassandra.enabled).isFalse();
  }

  @Test
  void shouldOverrideInstrumentationJdbcEnabled() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_JDBC_ENABLED", "false");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.instrumentation.jdbc.enabled).isFalse();
  }

  @Test
  void shouldOverrideInstrumentationJmsEnabled() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_JMS_ENABLED", "false");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.instrumentation.jms.enabled).isFalse();
  }

  @Test
  void shouldOverrideInstrumentationKafkaEnabled() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_KAFKA_ENABLED", "false");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.instrumentation.kafka.enabled).isFalse();
  }

  @Test
  void shouldOverrideInstrumentationMicrometerEnabled() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_MICROMETER_ENABLED", "false");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.instrumentation.micrometer.enabled).isFalse();
  }

  @Test
  void shouldOverrideInstrumentationMongoEnabled() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_MONGO_ENABLED", "false");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.instrumentation.mongo.enabled).isFalse();
  }

  @Test
  void shouldOverrideInstrumentationRabbitmqEnabled() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_RABBITMQ_ENABLED", "true");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.instrumentation.rabbitmq.enabled).isTrue();
  }

  @Test
  void shouldOverrideInstrumentationRedisEnabled() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_REDIS_ENABLED", "false");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.instrumentation.redis.enabled).isFalse();
  }

  @Test
  void shouldOverrideInstrumentationSpringSchedulingEnabled() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_INSTRUMENTATION_SPRING_SCHEDULING_ENABLED", "false");

    Configuration configuration = loadConfiguration();
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.instrumentation.springScheduling.enabled).isFalse();
  }

  @Test
  void shouldOverrideAadAuthenticationConfig() throws IOException {
    envVars.set("APPLICATIONINSIGHTS_AUTHENTICATION_STRING", "Authorization=AAD;ClientId=12345678");

    Configuration configuration = loadConfiguration("applicationinsights_aadauthenv.json");
    ConfigurationBuilder.overlayEnvVars(configuration);

    assertThat(configuration.preview.authentication.enabled).isTrue();
    assertThat(configuration.preview.authentication.type)
        .isEqualTo(Configuration.AuthenticationType.UAMI);
    assertThat(configuration.preview.authentication.clientId).isEqualTo("12345678");
    assertThat(configuration.preview.authentication.clientSecret).isNull();

    envVars.set("APPLICATIONINSIGHTS_AUTHENTICATION_STRING", "Authorization=AAD;ClientId=");

    Configuration configuration2 = loadConfiguration("applicationinsights_aadauthenv.json");
    ConfigurationBuilder.overlayEnvVars(configuration2);

    assertThat(configuration2.preview.authentication.enabled).isTrue();
    assertThat(configuration2.preview.authentication.type)
        .isEqualTo(Configuration.AuthenticationType.SAMI);
    assertThat(configuration2.preview.authentication.clientId).isNull();
    assertThat(configuration2.preview.authentication.clientSecret).isNull();
  }

  @Test
  void shouldUseRpConfigRole() {
    Configuration configuration = new Configuration();
    RpConfiguration rpConfiguration = new RpConfiguration();
    rpConfiguration.role.name = "role-name-from-rp";
    rpConfiguration.role.instance = "role-instance-from-rp";
    ConfigurationBuilder.overlayRpConfiguration(configuration, rpConfiguration);

    assertThat(configuration.role.name).isEqualTo("role-name-from-rp");
    assertThat(configuration.role.instance).isEqualTo("role-instance-from-rp");
  }

  @Test
  void shouldNotUseRpConfigRole() {
    Configuration configuration = new Configuration();
    configuration.role.name = "role-name";
    configuration.role.instance = "role-instance";
    RpConfiguration rpConfiguration = new RpConfiguration();
    rpConfiguration.role.name = "role-name-from-rp";
    rpConfiguration.role.instance = "role-instance-from-rp";
    ConfigurationBuilder.overlayRpConfiguration(configuration, rpConfiguration);

    assertThat(configuration.role.name).isEqualTo("role-name");
    assertThat(configuration.role.instance).isEqualTo("role-instance");
  }

  @Test
  void shouldNotParseFaultyJson() {
    assertThatThrownBy(() -> loadConfiguration("applicationinsights_faulty.json", true))
        .isInstanceOf(UnrecognizedPropertyException.class);
  }

  private static Configuration loadConfiguration() throws IOException {
    return loadConfiguration("applicationinsights.json");
  }

  private static Configuration loadConfiguration(String resourceName) throws IOException {
    return loadConfiguration(resourceName, false);
  }

  private static Configuration loadConfiguration(
      String resourceName, boolean failOnUnknownProperties) throws IOException {
    return new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties)
        .readValue(
            ConfigurationTest.class.getClassLoader().getResourceAsStream(resourceName),
            Configuration.class);
  }

  private static List<JmxMetric> parseJmxMetricsJson(String json) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(json, new TypeReference<List<JmxMetric>>() {});
  }
}
