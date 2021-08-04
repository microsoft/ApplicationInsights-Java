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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.common.HostName;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.JmxMetric;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.LoggerFactory;

public class ConfigurationBuilder {

  private static final String APPLICATIONINSIGHTS_CONFIGURATION_FILE =
      "APPLICATIONINSIGHTS_CONFIGURATION_FILE";
  private static final String APPLICATIONINSIGHTS_CONFIGURATION_CONTENT =
      "APPLICATIONINSIGHTS_CONFIGURATION_CONTENT";

  private static final String APPLICATIONINSIGHTS_CONNECTION_STRING_ENV =
      "APPLICATIONINSIGHTS_CONNECTION_STRING";

  private static final String APPLICATIONINSIGHTS_CONNECTION_STRING_SYS =
      "applicationinsights.connection.string";

  // this is for backwards compatibility only
  private static final String APPINSIGHTS_INSTRUMENTATIONKEY = "APPINSIGHTS_INSTRUMENTATIONKEY";

  private static final String APPLICATIONINSIGHTS_ROLE_NAME_ENV = "APPLICATIONINSIGHTS_ROLE_NAME";
  private static final String APPLICATIONINSIGHTS_ROLE_INSTANCE_ENV =
      "APPLICATIONINSIGHTS_ROLE_INSTANCE";

  private static final String APPLICATIONINSIGHTS_ROLE_NAME_SYS = "applicationinsights.role.name";
  private static final String APPLICATIONINSIGHTS_ROLE_INSTANCE_SYS =
      "applicationinsights.role.instance";

  // this is undocumented and will be removed in the future
  private static final String APPLICATIONINSIGHTS_JMX_METRICS = "APPLICATIONINSIGHTS_JMX_METRICS";

  private static final String APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE =
      "APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE";

  private static final String APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL =
      "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL";

  private static final String APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL =
      "APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL";
  public static final String APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH =
      "APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH";

  private static final String APPLICATIONINSIGHTS_PREVIEW_OTEL_API_SUPPORT =
      "APPLICATIONINSIGHTS_PREVIEW_OTEL_API_SUPPORT";
  private static final String APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_AZURE_SDK_ENABLED =
      "APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_AZURE_SDK_ENABLED";
  private static final String
      APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_SPRING_INTEGRATION_ENABLED =
          "APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_SPRING_INTEGRATION_ENABLED";

  private static final String APPLICATIONINSIGHTS_PREVIEW_LIVE_METRICS_ENABLED =
      "APPLICATIONINSIGHTS_PREVIEW_LIVE_METRICS_ENABLED";

  private static final String WEBSITE_SITE_NAME = "WEBSITE_SITE_NAME";
  private static final String WEBSITE_INSTANCE_ID = "WEBSITE_INSTANCE_ID";

  private static final String APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED =
      "APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED";

  private static final String APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS =
      "APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS";

  private static final String APPLICATIONINSIGHTS_AUTHENTICATION_STRING =
      "APPLICATIONINSIGHTS_AUTHENTICATION_STRING";

  // cannot use logger before loading configuration, so need to store warning messages locally until
  // logger is initialized
  private static final ConfigurationLogger configurationLogger = new ConfigurationLogger();

  // using deprecated fields to give warning message to user if they are still using them
  @SuppressWarnings("deprecation")
  public static Configuration create(Path agentJarPath, RpConfiguration rpConfiguration)
      throws IOException {
    Configuration config = loadConfigurationFile(agentJarPath);
    if (config.instrumentation.micrometer.reportingIntervalSeconds != 60) {
      configurationLogger.warn(
          "micrometer \"reportingIntervalSeconds\" setting leaked out previously"
              + " as an undocumented testing detail,"
              + " please use \"preview\": { \"metricIntervalSeconds\" } instead now"
              + " (and note that metricIntervalSeconds applies to all auto-collected metrics,"
              + " not only micrometer)");
    }
    if (config.preview.httpMethodInOperationName) {
      configurationLogger.warn(
          "\"httpMethodInOperationName\" preview setting is now the (one and only) default behavior");
    }
    if (config.preview.instrumentation.javaHttpClient.enabled) {
      configurationLogger.warn(
          "\"javaHttpClient\" instrumentation is no longer in preview"
              + " and it is now enabled by default,"
              + " so no need to enable it under preview configuration");
    }
    if (config.preview.instrumentation.jaxws.enabled) {
      configurationLogger.warn(
          "\"jaxws\" instrumentation is no longer in preview"
              + " and it is now enabled by default,"
              + " so no need to enable it under preview configuration");
    }
    if (config.preview.instrumentation.rabbitmq.enabled) {
      configurationLogger.warn(
          "\"rabbitmq\" instrumentation is no longer in preview"
              + " and it is now enabled by default,"
              + " so no need to enable it under preview configuration");
    }
    overlayEnvVars(config);
    applySamplingPercentageRounding(config);
    // rp configuration should always be last (so it takes precedence)
    // currently applicationinsights-rp.json is only used by Azure Spring Cloud
    if (rpConfiguration != null) {
      overlayRpConfiguration(config, rpConfiguration);
    }
    // only set role instance to host name as a last resort
    if (config.role.instance == null) {
      String hostname = HostName.get();
      config.role.instance = hostname == null ? "unknown" : hostname;
    }
    return config;
  }

  private static void overlayProfilerEnvVars(Configuration config) {
    config.preview.profiler.enabled =
        Boolean.parseBoolean(
            overlayWithEnvVar(
                APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED,
                Boolean.toString(config.preview.profiler.enabled)));
  }

  private static void overlayAadEnvVars(Configuration config) {
    String aadAuthString = getEnvVar(APPLICATIONINSIGHTS_AUTHENTICATION_STRING);
    if (aadAuthString != null) {
      Map<String, String> keyValueMap;
      try {
        keyValueMap = Strings.splitToMap(aadAuthString);
      } catch (IllegalArgumentException e) {
        throw new IllegalStateException(
            "Unable to parse APPLICATIONINSIGHTS_AUTHENTICATION_STRING environment variable: "
                + aadAuthString,
            e);
      }
      String authorization = keyValueMap.get("Authorization");
      if (authorization != null && authorization.equals("AAD")) {
        // Override any configuration from json
        config.preview.authentication = new Configuration.AadAuthentication();
        config.preview.authentication.enabled = true;
        config.preview.authentication.type = Configuration.AuthenticationType.SAMI;
        String clientId = keyValueMap.get("ClientId");
        if (clientId != null && !clientId.isEmpty()) {
          // Override type to User Assigned Managed Identity
          config.preview.authentication.type = Configuration.AuthenticationType.UAMI;
          config.preview.authentication.clientId = clientId;
        }
      }
    }
  }

  private static void loadLogCaptureEnvVar(Configuration config) {
    String loggingEnvVar = getEnvVar(APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL);
    if (loggingEnvVar != null) {
      config.instrumentation.logging.level = loggingEnvVar;
    }
  }

  // TODO deprecate this
  private static void loadJmxMetricsEnvVar(Configuration config) throws IOException {
    String jmxMetricsEnvVarJson = getEnvVar(APPLICATIONINSIGHTS_JMX_METRICS);

    // JmxMetrics env variable has higher precedence over jmxMetrics config from
    // applicationinsights.json
    if (jmxMetricsEnvVarJson != null && !jmxMetricsEnvVarJson.isEmpty()) {
      configurationLogger.warn(
          "The undocumented APPLICATIONINSIGHTS_JMX_METRICS environment variable support"
              + " has been deprecated, please use json file configuration instead");

      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      config.jmxMetrics =
          mapper.readValue(jmxMetricsEnvVarJson, new TypeReference<List<JmxMetric>>() {});
    }
  }

  private static void addDefaultJmxMetricsIfNotPresent(Configuration config) {
    if (!jmxMetricExists(config.jmxMetrics, "java.lang:type=Threading", "ThreadCount")) {
      JmxMetric threadCountJmxMetric = new JmxMetric();
      threadCountJmxMetric.name = "Current Thread Count";
      threadCountJmxMetric.objectName = "java.lang:type=Threading";
      threadCountJmxMetric.attribute = "ThreadCount";
      config.jmxMetrics.add(threadCountJmxMetric);
    }
    if (!jmxMetricExists(config.jmxMetrics, "java.lang:type=ClassLoading", "LoadedClassCount")) {
      JmxMetric classCountJmxMetric = new JmxMetric();
      classCountJmxMetric.name = "Loaded Class Count";
      classCountJmxMetric.objectName = "java.lang:type=ClassLoading";
      classCountJmxMetric.attribute = "LoadedClassCount";
      config.jmxMetrics.add(classCountJmxMetric);
    }
  }

  private static boolean jmxMetricExists(
      List<JmxMetric> jmxMetrics, String objectName, String attribute) {
    for (JmxMetric metric : jmxMetrics) {
      if (metric.objectName.equals(objectName) && metric.attribute.equals(attribute)) {
        return true;
      }
    }
    return false;
  }

  private static void overlayInstrumentationEnabledEnvVars(Configuration config) {
    config.instrumentation.cassandra.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_CASSANDRA_ENABLED",
            config.instrumentation.cassandra.enabled);
    config.instrumentation.jdbc.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_JDBC_ENABLED",
            config.instrumentation.jdbc.enabled);
    config.instrumentation.jms.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_JMS_ENABLED", config.instrumentation.jms.enabled);
    config.instrumentation.kafka.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_KAFKA_ENABLED",
            config.instrumentation.kafka.enabled);
    config.instrumentation.micrometer.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_MICROMETER_ENABLED",
            config.instrumentation.micrometer.enabled);
    config.instrumentation.mongo.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_MONGO_ENABLED",
            config.instrumentation.mongo.enabled);
    config.instrumentation.rabbitmq.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_RABBITMQ_ENABLED",
            config.instrumentation.rabbitmq.enabled);
    config.instrumentation.redis.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_REDIS_ENABLED",
            config.instrumentation.redis.enabled);
    config.instrumentation.springScheduling.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_SPRING_SCHEDULING_ENABLED",
            config.instrumentation.springScheduling.enabled);
  }

  private static Configuration loadConfigurationFile(Path agentJarPath) throws IOException {
    String configurationContent = getEnvVar(APPLICATIONINSIGHTS_CONFIGURATION_CONTENT);
    if (configurationContent != null) {
      return getConfigurationFromEnvVar(configurationContent, true);
    }

    String configPathStr = getConfigPath();
    if (configPathStr != null) {
      Path configPath = agentJarPath.resolveSibling(configPathStr);
      if (Files.exists(configPath)) {
        return loadJsonConfigFile(configPath);
      } else {
        // fail fast any time configuration is invalid
        throw new IllegalStateException(
            "could not find requested configuration file: " + configPathStr);
      }
    }

    if (DiagnosticsHelper.isRpIntegration()) {
      // users do not have write access to agent directory in rp integrations
      // and rp integrations should not use applicationinsights.json because that makes it difficult
      // to merge
      // rp intent and user intent
      return new Configuration();
    }

    Path configPath = agentJarPath.resolveSibling("applicationinsights.json");
    if (Files.exists(configPath)) {
      return loadJsonConfigFile(configPath);
    }

    if (Files.exists(agentJarPath.resolveSibling("ApplicationInsights.json"))) {
      throw new IllegalStateException(
          "found ApplicationInsights.json, but it should be lowercase: applicationinsights.json");
    }

    // json configuration file is not required, ok to configure via env var alone
    return new Configuration();
  }

  // cannot use logger before loading configuration, so need to store any messages locally until
  // logger is initialized
  public static void logConfigurationWarnMessages() {
    configurationLogger.log(LoggerFactory.getLogger(ConfigurationBuilder.class));
  }

  // visible for testing
  static void overlayEnvVars(Configuration config) throws IOException {
    config.connectionString =
        overlayWithSysPropEnvVar(
            APPLICATIONINSIGHTS_CONNECTION_STRING_SYS,
            APPLICATIONINSIGHTS_CONNECTION_STRING_ENV,
            config.connectionString);
    if (config.connectionString == null) {
      // this is for backwards compatibility only
      String instrumentationKey = getEnvVar(APPINSIGHTS_INSTRUMENTATIONKEY);
      if (instrumentationKey != null) {
        configurationLogger.warn(
            "APPINSIGHTS_INSTRUMENTATIONKEY is only supported for backwards compatibility,"
                + " please consider using APPLICATIONINSIGHTS_CONNECTION_STRING instead");
        config.connectionString = "InstrumentationKey=" + instrumentationKey;
      }
    }

    if (isTrimEmpty(config.role.name)) {
      // only use WEBSITE_SITE_NAME as a fallback
      config.role.name = getWebsiteSiteNameEnvVar();
    }
    config.role.name =
        overlayWithSysPropEnvVar(
            APPLICATIONINSIGHTS_ROLE_NAME_SYS, APPLICATIONINSIGHTS_ROLE_NAME_ENV, config.role.name);

    if (isTrimEmpty(config.role.instance)) {
      // only use WEBSITE_INSTANCE_ID as a fallback
      config.role.instance = getEnvVar(WEBSITE_INSTANCE_ID);
    }
    config.role.instance =
        overlayWithSysPropEnvVar(
            APPLICATIONINSIGHTS_ROLE_INSTANCE_SYS,
            APPLICATIONINSIGHTS_ROLE_INSTANCE_ENV,
            config.role.instance);

    config.sampling.percentage =
        overlayWithEnvVar(APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE, config.sampling.percentage);

    config.selfDiagnostics.level =
        overlayWithEnvVar(APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL, config.selfDiagnostics.level);
    config.selfDiagnostics.file.path =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH, config.selfDiagnostics.file.path);

    config.preview.metricIntervalSeconds =
        (int)
            overlayWithEnvVar(
                APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS,
                config.preview.metricIntervalSeconds);

    config.preview.openTelemetryApiSupport =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_PREVIEW_OTEL_API_SUPPORT, config.preview.openTelemetryApiSupport);
    config.preview.instrumentation.azureSdk.enabled =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_AZURE_SDK_ENABLED,
            config.preview.instrumentation.azureSdk.enabled);
    config.preview.instrumentation.springIntegration.enabled =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_SPRING_INTEGRATION_ENABLED,
            config.preview.instrumentation.springIntegration.enabled);

    config.preview.liveMetrics.enabled =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_PREVIEW_LIVE_METRICS_ENABLED, config.preview.liveMetrics.enabled);

    loadLogCaptureEnvVar(config);
    loadJmxMetricsEnvVar(config);

    addDefaultJmxMetricsIfNotPresent(config);
    overlayProfilerEnvVars(config);
    overlayAadEnvVars(config);
    overlayInstrumentationEnabledEnvVars(config);
  }

  public static void applySamplingPercentageRounding(Configuration config) {
    config.sampling.percentage = roundToNearest(config.sampling.percentage, true);
    for (SamplingOverride override : config.preview.sampling.overrides) {
      override.percentage = roundToNearest(override.percentage, true);
    }
  }

  // visible for testing
  static void overlayRpConfiguration(Configuration config, RpConfiguration rpConfiguration) {
    String connectionString = rpConfiguration.connectionString;
    if (!isTrimEmpty(connectionString)) {
      config.connectionString = connectionString;
    }
    if (rpConfiguration.sampling != null) {
      config.sampling.percentage = rpConfiguration.sampling.percentage;
    }
    if (rpConfiguration.ignoreRemoteParentNotSampled != null) {
      config.preview.ignoreRemoteParentNotSampled = rpConfiguration.ignoreRemoteParentNotSampled;
    }
    if (isTrimEmpty(config.role.name)) {
      // only use rp configuration role name as a fallback, similar to WEBSITE_SITE_NAME
      config.role.name = rpConfiguration.role.name;
    }
    if (isTrimEmpty(config.role.instance)) {
      // only use rp configuration role name as a fallback, similar to WEBSITE_INSTANCE_ID
      config.role.instance = rpConfiguration.role.instance;
    }
  }

  private static String getConfigPath() {
    String value = getEnvVar(APPLICATIONINSIGHTS_CONFIGURATION_FILE);
    if (value != null) {
      return value;
    }
    // intentionally not checking system properties for other system properties
    // with the intention to keep configuration paths minimal to help with supportability
    return trimAndEmptyToNull(System.getProperty("applicationinsights.configuration.file"));
  }

  private static String getWebsiteSiteNameEnvVar() {
    String value = getEnvVar(WEBSITE_SITE_NAME);
    // TODO what is the best way to identify running as Azure Functions worker?
    // TODO is this the correct way to match role name from Azure Functions IIS host?
    if (value != null && "java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"))) {
      // special case for Azure Functions
      return value.toLowerCase(Locale.ENGLISH);
    }
    return value;
  }

  public static String overlayWithSysPropEnvVar(
      String systemPropertyName, String envVarName, String defaultValue) {
    String value = getSystemProperty(systemPropertyName);
    if (value != null) {
      configurationLogger.debug("using system property: {}", systemPropertyName);
      return value;
    }
    return overlayWithEnvVar(envVarName, defaultValue);
  }

  public static String overlayWithEnvVar(String name, String defaultValue) {
    String value = getEnvVar(name);
    if (value != null) {
      configurationLogger.debug("using environment variable: {}", name);
      return value;
    }
    return defaultValue;
  }

  static float overlayWithEnvVar(String name, float defaultValue) {
    String value = getEnvVar(name);
    if (value != null) {
      configurationLogger.debug("using environment variable: {}", name);
      // intentionally allowing NumberFormatException to bubble up as invalid configuration and
      // prevent agent from starting
      return Float.parseFloat(value);
    }
    return defaultValue;
  }

  static boolean overlayWithEnvVar(String name, boolean defaultValue) {
    String value = getEnvVar(name);
    if (value != null) {
      configurationLogger.debug("using environment variable: {}", name);
      return Boolean.parseBoolean(value);
    }
    return defaultValue;
  }

  // never returns empty string (empty string is normalized to null)
  protected static String getSystemProperty(String name) {
    return trimAndEmptyToNull(System.getProperty(name));
  }

  // never returns empty string (empty string is normalized to null)
  protected static String getEnvVar(String name) {
    return trimAndEmptyToNull(System.getenv(name));
  }

  // visible for testing
  static String trimAndEmptyToNull(String str) {
    if (str == null) {
      return null;
    }
    String trimmed = str.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static boolean isTrimEmpty(String value) {
    return value == null || value.trim().isEmpty();
  }

  public static class ConfigurationException extends RuntimeException {

    ConfigurationException(String message, Exception e) {
      super(message, e);
    }

    ConfigurationException(String message) {
      super(message);
    }
  }

  static Configuration getConfigurationFromConfigFile(Path configPath, boolean strict)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    try (InputStream in = Files.newInputStream(configPath)) {
      if (!strict) {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      }
      return mapper.readValue(in, Configuration.class);
    } catch (UnrecognizedPropertyException ex) {
      if (strict) {
        Configuration configuration = getConfigurationFromConfigFile(configPath, false);
        configurationLogger.warn(
            getJsonEncodingExceptionMessageForFile(configPath, ex.getMessage()));
        return configuration;
      } else {
        throw new FriendlyException(
            getJsonEncodingExceptionMessageForFile(configPath, ex.getMessage()),
            "Learn more about configuration options here: https://go.microsoft.com/fwlink/?linkid=2153358");
      }
    } catch (JsonMappingException | JsonParseException ex) {
      throw new FriendlyException(
          "Application Insights Java agent failed to start",
          "Learn more about configuration options here: https://go.microsoft.com/fwlink/?linkid=2153358",
          ex.getMessage(),
          "Error parsing configuration from file: " + configPath.toAbsolutePath());
    } catch (Exception e) {
      throw new ConfigurationException(
          "Error parsing configuration from file: " + configPath.toAbsolutePath(), e);
    }
  }

  static Configuration getConfigurationFromEnvVar(String content, boolean strict) {
    Configuration configuration;
    ObjectMapper mapper = new ObjectMapper();
    if (!strict) {
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    try {
      configuration = mapper.readValue(content, Configuration.class);
    } catch (UnrecognizedPropertyException ex) {
      if (strict) {
        // Try extracting the configuration without failOnUnknown
        configuration = getConfigurationFromEnvVar(content, false);
        // cannot use logger before loading configuration, so need to store warning messages locally
        // until logger is initialized
        configurationLogger.warn(getJsonEncodingExceptionMessageForEnvVar(ex.getMessage()));
      } else {
        throw new FriendlyException(
            getJsonEncodingExceptionMessageForEnvVar(ex.getMessage()),
            "Learn more about configuration options here: https://go.microsoft.com/fwlink/?linkid=2153358");
      }
    } catch (JsonMappingException | JsonParseException ex) {
      throw new FriendlyException(
          getJsonEncodingExceptionMessageForEnvVar(ex.getMessage()),
          "Learn more about configuration options here: https://go.microsoft.com/fwlink/?linkid=2153358");
    } catch (Exception e) {
      throw new ConfigurationException(
          "Error parsing configuration from env var: " + APPLICATIONINSIGHTS_CONFIGURATION_CONTENT,
          e);
    }

    if (configuration.connectionString != null) {
      throw new ConfigurationException(
          "\"connectionString\" attribute is not supported inside of "
              + APPLICATIONINSIGHTS_CONFIGURATION_CONTENT
              + ", please use "
              + APPLICATIONINSIGHTS_CONNECTION_STRING_ENV
              + " to specify the connection string");
    }
    return configuration;
  }

  static String getJsonEncodingExceptionMessageForFile(Path configPath, String message) {
    return getJsonEncodingExceptionMessage("file " + configPath.toAbsolutePath(), message);
  }

  static String getJsonEncodingExceptionMessageForEnvVar(String message) {
    return getJsonEncodingExceptionMessage(
        "env var " + APPLICATIONINSIGHTS_CONFIGURATION_CONTENT, message);
  }

  static String getJsonEncodingExceptionMessage(String location, String message) {
    String defaultMessage =
        "Application Insights Java agent's configuration " + location + " has a malformed JSON\n";
    if (message == null || message.isEmpty()) {
      return defaultMessage;
    }
    return message;
  }

  public static Configuration loadJsonConfigFile(Path configPath) throws IOException {
    if (!Files.exists(configPath)) {
      throw new IllegalStateException("config file does not exist: " + configPath);
    }
    Configuration configuration = getConfigurationFromConfigFile(configPath, true);
    if (configuration.instrumentationSettings != null) {
      throw new IllegalStateException(
          "It looks like you are using an old applicationinsights.json file"
              + " which still has \"instrumentationSettings\", please see the docs for the new format:"
              + " https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config");
    }
    return configuration;
  }

  // this is for external callers, where logging is ok
  public static float roundToNearest(float samplingPercentage) {
    return roundToNearest(samplingPercentage, false);
  }

  // visible for testing
  private static float roundToNearest(float samplingPercentage, boolean doNotLogWarnMessages) {
    if (samplingPercentage == 0) {
      return 0;
    }
    double itemCount = 100 / samplingPercentage;
    float rounded = 100.0f / Math.round(itemCount);

    if (Math.abs(samplingPercentage - rounded) >= 1) {
      // TODO include link to docs in this warning message
      if (doNotLogWarnMessages) {
        configurationLogger.warn(
            "the requested sampling percentage {} was rounded to nearest 100/N: {}",
            samplingPercentage,
            rounded);
      } else {
        // this is the "startup logger"
        LoggerFactory.getLogger("com.microsoft.applicationinsights.agent")
            .warn(
                "the requested sampling percentage {} was rounded to nearest 100/N: {}",
                samplingPercentage,
                rounded);
      }
    }

    return rounded;
  }

  private ConfigurationBuilder() {}
}
