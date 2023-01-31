// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import com.azure.monitor.opentelemetry.exporter.implementation.utils.HostName;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ConnectionStringOverride;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.InstrumentationKeyOverride;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.JmxMetric;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.slf4j.LoggerFactory;

public class ConfigurationBuilder {

  private static final String APPLICATIONINSIGHTS_CONFIGURATION_FILE =
      "APPLICATIONINSIGHTS_CONFIGURATION_FILE";
  private static final String APPLICATIONINSIGHTS_CONFIGURATION_CONTENT =
      "APPLICATIONINSIGHTS_CONFIGURATION_CONTENT";

  private static final String APPLICATIONINSIGHTS_RUNTIME_ATTACHED_CONFIGURATION_CONTENT =
      "applicationinsights.internal.runtime.attached.json";

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

  private static final String APPLICATIONINSIGHTS_SAMPLING_REQUESTS_PER_SECOND =
      "APPLICATIONINSIGHTS_SAMPLING_LIMIT_PER_SECOND";

  private static final String APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL =
      "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL";

  // not recommending Azure SDK's HTTPS_PROXY because it requires also setting
  // -Djava.net.useSystemProxies=true
  private static final String APPLICATIONINSIGHTS_PROXY = "APPLICATIONINSIGHTS_PROXY";

  private static final String APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL =
      "APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL";
  public static final String APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH =
      "APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH";

  private static final String
      APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_SPRING_INTEGRATION_ENABLED =
          "APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_SPRING_INTEGRATION_ENABLED";

  private static final String APPLICATIONINSIGHTS_PREVIEW_LIVE_METRICS_ENABLED =
      "APPLICATIONINSIGHTS_PREVIEW_LIVE_METRICS_ENABLED";

  private static final String WEBSITE_SITE_NAME = "WEBSITE_SITE_NAME";
  private static final String WEBSITE_INSTANCE_ID = "WEBSITE_INSTANCE_ID";

  private static final String APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED =
      "APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED";

  private static final String APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLEDIAGNOSTICS =
      "APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLEDIAGNOSTICS";

  @Deprecated
  private static final String APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS =
      "APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS";

  private static final String APPLICATIONINSIGHTS_METRIC_INTERVAL_SECONDS =
      "APPLICATIONINSIGHTS_METRIC_INTERVAL_SECONDS";

  private static final String APPLICATIONINSIGHTS_AUTHENTICATION_STRING =
      "APPLICATIONINSIGHTS_AUTHENTICATION_STRING";

  private static final String APPLICATIONINSIGHTS_STATSBEAT_DISABLED =
      "APPLICATIONINSIGHTS_STATSBEAT_DISABLED";

  // cannot use logger before loading configuration, so need to store warning messages locally until
  // logger is initialized
  private static final ConfigurationLogger configurationLogger = new ConfigurationLogger();
  public static final String CONFIGURATION_OPTIONS_LINK =
      "https://go.microsoft.com/fwlink/?linkid=2153358";

  // using deprecated fields to give warning message to user if they are still using them
  public static Configuration create(Path agentJarPath, @Nullable RpConfiguration rpConfiguration)
      throws IOException {
    Configuration config = loadConfigurationFile(agentJarPath);
    logConfigurationWarnings(config);
    overlayConfiguration(agentJarPath, rpConfiguration, config);
    return config;
  }

  private static void logConfigurationWarnings(Configuration config) {
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
          "\"httpMethodInOperationName\" is no longer in preview and it is now the"
              + " (one and only) default behavior");
    }
    if (config.preview.ignoreRemoteParentNotSampled != null) {
      configurationLogger.warn(
          "\"ignoreRemoteParentNotSampled\" has been deprecated and"
              + " \"ignoreRemoteParentNotSampled\": false has replaced with"
              + " \"preview\": { \"sampling\": { \"parentBased\": true } } (while"
              + " \"ignoreRemoteParentNotSampled\": true is just the default behavior)");
    }
    if (config.preview.openTelemetryApiSupport) {
      configurationLogger.warn(
          "\"openTelemetryApiSupport\" is no longer in preview and it is now the"
              + " (one and only) default behavior");
    }
    if (config.preview.metricIntervalSeconds != 60) {
      configurationLogger.warn(
          "\"metricIntervalSeconds\" is no longer in preview and it has been GA since 3.4.9");
      if (config.metricIntervalSeconds == 60) {
        config.metricIntervalSeconds = config.preview.metricIntervalSeconds;
      }
    }
    if (config.preview.instrumentation.azureSdk.enabled) {
      configurationLogger.warn(
          "\"azureSdk\" instrumentation is no longer in preview"
              + " and it is now enabled by default,"
              + " so no need to enable it under preview configuration");
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
    if (config.preview.instrumentation.quartz.enabled) {
      configurationLogger.warn(
          "\"quartz\" instrumentation is no longer in preview"
              + " and it is now enabled by default,"
              + " so no need to enable it under preview configuration");
    }
    if (config.preview.instrumentation.rabbitmq.enabled) {
      configurationLogger.warn(
          "\"rabbitmq\" instrumentation is no longer in preview"
              + " and it is now enabled by default,"
              + " so no need to enable it under preview configuration");
    }
    for (SamplingOverride override : config.preview.sampling.overrides) {
      if (override.telemetryKind != null) {
        configurationLogger.warn(
            "Sampling overrides \"telemetryKind\" has been deprecated,"
                + " and support for it will be removed in a future release, please transition from"
                + " \"telemetryKind\" to \"telemetryType\".");
        if (override.telemetryType == null) {
          override.telemetryType = override.telemetryKind;
        }
      }
      if (override.spanKind != null) {
        configurationLogger.warn(
            "Sampling overrides \"spanKind\" has been deprecated,"
                + " and support for it will be removed in a future release, please transition from"
                + " \"spanKind\" to \"telemetryType\".");
      }
      if (override.telemetryType == null) {
        configurationLogger.warn(
            "Sampling overrides \"telemetryType\" is missing,"
                + " and will be required in a future release, please transition to add"
                + " \"telemetryType\" for sampling overrides.");
      }
      if (override.includingStandaloneTelemetry != null) {
        configurationLogger.warn(
            "Sampling overrides \"includingStandaloneTelemetry\" (from 3.4.0-BETA) has been"
                + " removed in 3.4.0 (GA)");
      }
    }
    if (!config.preview.instrumentationKeyOverrides.isEmpty()) {
      configurationLogger.warn(
          "Instrumentation key overrides have been deprecated,"
              + " and support for it will be removed in a future release, please transition from"
              + " \"instrumentationKeyOverrides\" to \"connectionStringOverrides\".");
      for (InstrumentationKeyOverride override : config.preview.instrumentationKeyOverrides) {
        ConnectionStringOverride newOverride = new ConnectionStringOverride();
        newOverride.httpPathPrefix = override.httpPathPrefix;
        newOverride.connectionString = "InstrumentationKey=" + override.instrumentationKey;
        config.preview.connectionStringOverrides.add(newOverride);
      }
    }
    if (config.sampling.limitPerSecond != null) {
      configurationLogger.warn(
          "\"limitPerSecond\" (from 3.4.0-BETA) has been renamed to \"requestsPerSecond\""
              + " in 3.4.0 (GA)");
      if (config.sampling.requestsPerSecond == null && config.sampling.percentage == null) {
        config.sampling.requestsPerSecond = config.sampling.limitPerSecond;
      }
    }

    logWarningIfUsingInternalAttributes(config);
  }

  private static void overlayConfiguration(
      Path agentJarPath, RpConfiguration rpConfiguration, Configuration config) throws IOException {
    overlayFromEnv(config, agentJarPath.getParent());
    config.sampling.percentage = roundToNearest(config.sampling.percentage, true);
    for (SamplingOverride override : config.preview.sampling.overrides) {
      override.percentage = roundToNearest(override.percentage, true);
    }
    // rp configuration should always be last (so it takes precedence)
    // currently applicationinsights-rp.json is only used by Azure Spring Cloud
    if (rpConfiguration != null) {
      overlayFromEnv(rpConfiguration);
      overlayRpConfiguration(config, rpConfiguration);
    }
    // only fall back to default sampling configuration after all overlays have been performed
    if (config.sampling.requestsPerSecond == null && config.sampling.percentage == null) {
      config.sampling.requestsPerSecond = 5.0;
    }
    // only set role instance to host name as a last resort
    if (config.role.instance == null) {
      String hostname = HostName.get();
      config.role.instance = hostname == null ? "unknown" : hostname;
    }
  }

  private static void logWarningIfUsingInternalAttributes(Configuration config) {
    for (Configuration.ProcessorConfig processor : config.preview.processors) {
      if (processor.include != null) {
        logWarningIfUsingInternalAttributes(processor.include);
      }
      if (processor.exclude != null) {
        logWarningIfUsingInternalAttributes(processor.exclude);
      }
      for (Configuration.ProcessorAction action : processor.actions) {
        if (action.key != null) {
          logWarningIfUsingInternalAttributes(action.key.getKey());
        }
        if (action.fromAttribute != null) {
          logWarningIfUsingInternalAttributes(action.fromAttribute.getKey());
        }
      }
    }
    for (SamplingOverride override : config.preview.sampling.overrides) {
      for (Configuration.SamplingOverrideAttribute attribute : override.attributes) {
        logWarningIfUsingInternalAttributes(attribute.key);
      }
    }
  }

  private static void logWarningIfUsingInternalAttributes(
      Configuration.ProcessorIncludeExclude includeExclude) {
    for (Configuration.ProcessorAttribute attribute : includeExclude.attributes) {
      logWarningIfUsingInternalAttributes(attribute.key);
    }
  }

  private static void logWarningIfUsingInternalAttributes(String attributeKey) {
    if (attributeKey.startsWith("applicationinsights.internal.")) {
      configurationLogger.warn(
          "Usage of internal attributes in processor configurations is not supported"
              + " and will be removed in a future version: "
              + attributeKey);
    }
  }

  static void overlayProfilerEnvVars(Configuration config) {
    String enabledString = Boolean.toString(config.preview.profiler.enabled);

    String overlayedValue =
        overlayWithEnvVar(APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED, enabledString);

    if (overlayedValue != null) {
      config.preview.profiler.enabled = Boolean.parseBoolean(overlayedValue);
    }

    if (config.preview.profiler.enabled && isOpenJ9Jvm()) {
      configurationLogger.warn(
          "Profiler is not supported for an OpenJ9 JVM. Instead, please use an OpenJDK JVM.");
      config.preview.profiler.enabled = false;
    }

    config.preview.profiler.enableDiagnostics =
        Boolean.parseBoolean(
            overlayWithEnvVar(
                APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLEDIAGNOSTICS,
                Boolean.toString(config.preview.profiler.enableDiagnostics)));
  }

  private static boolean isOpenJ9Jvm() {
    String jvmName = System.getProperty("java.vm.name");
    return jvmName != null && jvmName.contains("OpenJ9");
  }

  private static void overlayAadEnvVars(Configuration config) {
    String aadAuthString = getEnvVar(APPLICATIONINSIGHTS_AUTHENTICATION_STRING);
    if (aadAuthString != null) {
      Map<String, String> keyValueMap;
      try {
        keyValueMap = Strings.splitToMap(aadAuthString);
      } catch (IllegalArgumentException e) {
        throw new ConfigurationException(
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
    config.instrumentation.azureSdk.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_AZURE_SDK_ENABLED",
            config.instrumentation.azureSdk.enabled);
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

  private static Configuration loadConfigurationFile(Path agentJarPath) {
    String configurationContent = getEnvVar(APPLICATIONINSIGHTS_CONFIGURATION_CONTENT);
    if (configurationContent != null) {
      return getConfigurationFromEnvVar(configurationContent);
    }

    Configuration configFromProperty = extractConfigFromProperty(agentJarPath);
    if (configFromProperty != null) {
      return configFromProperty;
    }

    String runtimeAttachedConfigurationContent =
        getSystemProperty(APPLICATIONINSIGHTS_RUNTIME_ATTACHED_CONFIGURATION_CONTENT);
    if (runtimeAttachedConfigurationContent != null) {
      return getConfiguration(runtimeAttachedConfigurationContent, JsonOrigin.RUNTIME_ATTACHED);
    }

    if (DiagnosticsHelper.isRpIntegration()) {
      // users do not have write access to agent directory in rp integrations
      // and rp integrations should not use applicationinsights.json because that makes it difficult
      // to merge rp intent and user intent
      return new Configuration();
    }

    Configuration configFromJsonNextToAgent = extractConfigFromJsonNextToAgentJar(agentJarPath);
    if (configFromJsonNextToAgent != null) {
      return configFromJsonNextToAgent;
    }

    // json configuration file is not required, ok to configure via env var alone
    return new Configuration();
  }

  @Nullable
  private static Configuration extractConfigFromProperty(Path agentJarPath) {
    String configPathStr = getConfigPath();
    if (configPathStr != null) {
      Path configPath = agentJarPath.resolveSibling(configPathStr);
      if (Files.exists(configPath)) {
        return loadJsonConfigFile(configPath);
      } else {
        // fail fast any time configuration is invalid
        throw new ConfigurationException(
            "could not find requested configuration file: " + configPathStr);
      }
    }
    return null;
  }

  @Nullable
  private static Configuration extractConfigFromJsonNextToAgentJar(Path agentJarPath) {
    Path configPath = agentJarPath.resolveSibling("applicationinsights.json");
    if (Files.exists(configPath)) {
      return loadJsonConfigFile(configPath);
    }
    if (Files.exists(agentJarPath.resolveSibling("ApplicationInsights.json"))) {
      throw new ConfigurationException(
          "found ApplicationInsights.json, but it should be lowercase: applicationinsights.json");
    }
    return null;
  }

  // cannot use logger before loading configuration, so need to store any messages locally until
  // logger is initialized
  public static void logConfigurationWarnMessages() {
    configurationLogger.log(LoggerFactory.getLogger(ConfigurationBuilder.class));
  }

  // visible for testing
  static void overlayFromEnv(Configuration config, Path baseDir) throws IOException {
    // load connection string from a file if connection string is in the format of
    // "${file:mounted_connection_string_file.txt}"
    Map<String, StringLookup> stringLookupMap =
        Collections.singletonMap(StringLookupFactory.KEY_FILE, new FileStringLookup(baseDir));
    StringLookup stringLookup =
        StringLookupFactory.INSTANCE.interpolatorStringLookup(stringLookupMap, null, false);
    StringSubstitutor stringSubstitutor = new StringSubstitutor(stringLookup);
    config.connectionString =
        overlayConnectionStringFromEnv(stringSubstitutor.replace(config.connectionString));
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

    config.sampling.requestsPerSecond =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_SAMPLING_REQUESTS_PER_SECOND, config.sampling.requestsPerSecond);

    config.proxy = overlayProxyFromEnv(config.proxy);

    config.selfDiagnostics.level =
        overlayWithEnvVar(APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL, config.selfDiagnostics.level);
    config.selfDiagnostics.file.path =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH, config.selfDiagnostics.file.path);

    String deprecatedMetricIntervalSeconds =
        getEnvVar(APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS);
    String metricIntervalSeconds = getEnvVar(APPLICATIONINSIGHTS_METRIC_INTERVAL_SECONDS);
    if (metricIntervalSeconds != null) {
      config.metricIntervalSeconds =
          overlayWithEnvVar(
              APPLICATIONINSIGHTS_METRIC_INTERVAL_SECONDS, config.metricIntervalSeconds);
    } else if (deprecatedMetricIntervalSeconds != null) {
      configurationLogger.warn(
          "\"APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS\" has been renamed to \"APPLICATIONINSIGHTS_METRIC_INTERVAL_SECONDS\""
              + " in 3.4.9 (GA)");
      config.metricIntervalSeconds =
          overlayWithEnvVar(
              APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS, config.metricIntervalSeconds);
    }

    config.preview.instrumentation.springIntegration.enabled =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_SPRING_INTEGRATION_ENABLED,
            config.preview.instrumentation.springIntegration.enabled);

    config.preview.liveMetrics.enabled =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_PREVIEW_LIVE_METRICS_ENABLED, config.preview.liveMetrics.enabled);

    config.preview.statsbeat.disabled =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_STATSBEAT_DISABLED, config.preview.statsbeat.disabled);

    loadLogCaptureEnvVar(config);
    loadJmxMetricsEnvVar(config);

    addDefaultJmxMetricsIfNotPresent(config);
    overlayProfilerEnvVars(config);
    overlayAadEnvVars(config);
    overlayInstrumentationEnabledEnvVars(config);
  }

  public static void overlayFromEnv(RpConfiguration config) {
    config.connectionString = overlayConnectionStringFromEnv(config.connectionString);
    config.sampling.percentage =
        overlayWithEnvVar(APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE, config.sampling.percentage);
  }

  @Nullable
  private static String overlayConnectionStringFromEnv(String connectionString) {
    String value =
        overlayWithSysPropEnvVar(
            APPLICATIONINSIGHTS_CONNECTION_STRING_SYS,
            APPLICATIONINSIGHTS_CONNECTION_STRING_ENV,
            connectionString);

    if (value != null) {
      return value;
    }

    // this is for backwards compatibility only
    String instrumentationKey = getEnvVar(APPINSIGHTS_INSTRUMENTATIONKEY);
    if (instrumentationKey != null) {
      configurationLogger.warn(
          "APPINSIGHTS_INSTRUMENTATIONKEY is only supported for backwards compatibility,"
              + " please consider using APPLICATIONINSIGHTS_CONNECTION_STRING instead");
      return "InstrumentationKey=" + instrumentationKey;
    }

    return null;
  }

  private static Configuration.Proxy overlayProxyFromEnv(Configuration.Proxy proxy) {

    String proxyEnvVar = getEnvVar(APPLICATIONINSIGHTS_PROXY);
    if (proxyEnvVar == null) {
      if (proxy.password != null) {
        configurationLogger.warn(
            "Storing the proxy password in the application insights json configuration file"
                + " is deprecated because it is not secure. Please use the"
                + " APPLICATIONINSIGHTS_PROXY environment variable instead which supports passing"
                + " the username and password, e.g."
                + " APPLICATIONINSIGHTS_PROXY=https://myuser:mypassword@myproxy:8888");
      }
      return proxy;
    }

    Configuration.Proxy proxyFromEnv = new Configuration.Proxy();

    try {
      URL proxyUrl = new URL(proxyEnvVar);
      proxyFromEnv.host = proxyUrl.getHost();
      proxyFromEnv.port = proxyUrl.getPort();
      if (proxyFromEnv.port == -1) {
        proxyFromEnv.port = proxyUrl.getDefaultPort();
      }

      String userInfo = proxyUrl.getUserInfo();
      if (userInfo != null) {
        String[] usernamePassword = userInfo.split(":", 2);
        if (usernamePassword.length == 2) {
          proxyFromEnv.username =
              URLDecoder.decode(usernamePassword[0], StandardCharsets.UTF_8.toString());
          proxyFromEnv.password =
              URLDecoder.decode(usernamePassword[1], StandardCharsets.UTF_8.toString());
        }
      }
    } catch (IOException e) {
      throw new FriendlyException(
          "Error parsing environment variable APPLICATIONINSIGHTS_PROXY",
          "Learn more about configuration options here: " + CONFIGURATION_OPTIONS_LINK,
          e);
    }

    return proxyFromEnv;
  }

  // visible for testing
  static void overlayRpConfiguration(Configuration config, RpConfiguration rpConfiguration) {
    String connectionString = rpConfiguration.connectionString;
    if (!isTrimEmpty(connectionString)) {
      config.connectionString = connectionString;
    }
    if (rpConfiguration.sampling != null) {
      config.sampling.percentage = rpConfiguration.sampling.percentage;
      config.sampling.requestsPerSecond = rpConfiguration.sampling.requestsPerSecond;
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
    String configPath = getEnvVar(APPLICATIONINSIGHTS_CONFIGURATION_FILE);
    if (configPath != null) {
      return configPath;
    }
    // intentionally not checking system properties for other system properties
    // with the intention to keep configuration paths minimal to help with supportability
    return getSystemProperty("applicationinsights.configuration.file");
  }

  private static String getWebsiteSiteNameEnvVar() {
    String websiteSiteName = getEnvVar(WEBSITE_SITE_NAME);
    if (websiteSiteName != null && inAzureFunctionsWorker()) {
      // special case for Azure Functions
      return websiteSiteName.toLowerCase(Locale.ROOT);
    }
    return websiteSiteName;
  }

  public static boolean inAzureFunctionsConsumptionWorker() {
    // for now its the same, but in future should be different check
    return inAzureFunctionsWorker();
  }

  public static boolean inAzureFunctionsWorker() {
    // supporting both Azure Functions RP Integration, as well as bring your own agent deployments
    // in Azure Functions
    return "java".equals(System.getenv("FUNCTIONS_WORKER_RUNTIME"));
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
      return value;
    }
    return defaultValue;
  }

  @Nullable
  static Double overlayWithEnvVar(String name, @Nullable Double defaultValue) {
    String value = getEnvVar(name);
    if (value != null) {
      configurationLogger.debug("applying environment variable: {}={}", name, value);
      // intentionally allowing NumberFormatException to bubble up as invalid configuration and
      // prevent agent from starting
      return Double.parseDouble(value);
    }
    return defaultValue;
  }

  static int overlayWithEnvVar(String name, int defaultValue) {
    String value = getEnvVar(name);
    if (value != null) {
      configurationLogger.debug("using environment variable: {}", name);
      // intentionally allowing NumberFormatException to bubble up as invalid configuration and
      // prevent agent from starting
      return Integer.parseInt(value);
    }
    return defaultValue;
  }

  static boolean overlayWithEnvVar(String name, boolean defaultValue) {
    String value = getEnvVar(name);
    if (value != null) {
      configurationLogger.debug("applying environment variable: {}={}", name, value);
      return Boolean.parseBoolean(value);
    }
    return defaultValue;
  }

  // never returns empty string (empty string is normalized to null)
  protected static String getSystemProperty(String name) {
    String value = Strings.trimAndEmptyToNull(System.getProperty(name));
    if (value != null) {
      configurationLogger.debug("read system property: {}={}", name, value);
    }
    return value;
  }

  // never returns empty string (empty string is normalized to null)
  protected static String getEnvVar(String name) {
    String value = Strings.trimAndEmptyToNull(System.getenv(name));
    if (value != null) {
      configurationLogger.debug("read environment variable: {}={}", name, value);
    }
    return value;
  }

  private static boolean isTrimEmpty(@Nullable String value) {
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

  private static class JsonOrigin {

    private static final JsonOrigin ENV_VAR =
        new JsonOrigin("env var " + APPLICATIONINSIGHTS_CONFIGURATION_CONTENT);
    private static final JsonOrigin RUNTIME_ATTACHED =
        new JsonOrigin("JSON file coming from runtime attachment");

    private final String description;

    private static JsonOrigin fromPath(Path configPath) {
      return new JsonOrigin("file " + configPath.toAbsolutePath());
    }

    private JsonOrigin(String description) {
      this.description = description;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  static Configuration getConfigurationFromEnvVar(String json) {

    Configuration configuration = getConfiguration(json, JsonOrigin.ENV_VAR);

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

  private static Configuration loadJsonConfigFile(Path configPath) {
    if (!Files.exists(configPath)) {
      throw new ConfigurationException("config file does not exist: " + configPath);
    }
    Configuration configuration = getConfigurationFromConfigFile(configPath);
    if (configuration.instrumentationSettings != null) {
      throw new ConfigurationException(
          "It looks like you are using an old applicationinsights.json file"
              + " which still has \"instrumentationSettings\", please see the docs for the new format:"
              + " https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-config");
    }
    return configuration;
  }

  // visible for testing
  static Configuration getConfigurationFromConfigFile(Path configPath) {
    JsonOrigin jsonOrigin = JsonOrigin.fromPath(configPath);
    JsonNode jsonNode;
    try {
      jsonNode = new ObjectMapper().readTree(configPath.toFile());
    } catch (JsonProcessingException e) {
      throw createMalformedJsonFriendlyException(e, jsonOrigin);
    } catch (IOException e) {
      throw new ConfigurationException(
          "Error reading configuration file: " + configPath.toAbsolutePath(), e);
    }

    return getConfiguration(jsonNode, jsonOrigin, true);
  }

  private static Configuration getConfiguration(String json, JsonOrigin jsonOrigin) {
    JsonNode jsonNode;
    try {
      jsonNode = new ObjectMapper().readTree(json);
    } catch (JsonProcessingException e) {
      throw createMalformedJsonFriendlyException(e, jsonOrigin);
    }
    return getConfiguration(jsonNode, jsonOrigin, true);
  }

  private static Configuration getConfiguration(
      JsonNode jsonNode, JsonOrigin jsonOrigin, boolean strict) {
    configurationLogger.debug("configuration: {}", jsonNode);
    ObjectMapper mapper = new ObjectMapper();
    if (!strict) {
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    try {
      return mapper.treeToValue(jsonNode, Configuration.class);
    } catch (UnrecognizedPropertyException e) {
      if (strict) {
        Configuration configuration = getConfiguration(jsonNode, jsonOrigin, false);
        configurationLogger.warn(getJsonEncodingExceptionMessage(e.getMessage(), jsonOrigin), e);
        return configuration;
      } else {
        throw new FriendlyException(
            getJsonEncodingExceptionMessage(e.getMessage(), jsonOrigin),
            "Learn more about configuration options here: " + CONFIGURATION_OPTIONS_LINK);
      }
    } catch (JsonParseException | JsonMappingException e) {
      throw new FriendlyException(
          "Error parsing configuration from "
              + jsonOrigin
              + "."
              + System.lineSeparator()
              + System.lineSeparator()
              + e.getMessage(),
          "Learn more about configuration options here: " + CONFIGURATION_OPTIONS_LINK,
          e);
    } catch (Exception e) {
      throw new ConfigurationException("Error parsing configuration from " + jsonOrigin, e);
    }
  }

  private static FriendlyException createMalformedJsonFriendlyException(
      JsonProcessingException e, JsonOrigin jsonOrigin) {
    return new FriendlyException(
        "The configuration "
            + jsonOrigin
            + " contains malformed JSON."
            + System.lineSeparator()
            + System.lineSeparator()
            + e.getMessage(),
        "Learn more about configuration options here: " + CONFIGURATION_OPTIONS_LINK,
        e);
  }

  static String getJsonEncodingExceptionMessage(@Nullable String message, String location) {
    if (message != null && !message.isEmpty()) {
      return message;
    }
    return "The configuration " + location + " contains malformed JSON";
  }

  static String getJsonEncodingExceptionMessage(String message, JsonOrigin jsonOrigin) {
    if (message != null && !message.isEmpty()) {
      return message;
    }
    return "The configuration " + jsonOrigin + " contains malformed JSON";
  }

  // this is for external callers, where logging is ok
  public static double roundToNearest(double samplingPercentage) {
    return roundToNearest(samplingPercentage, false);
  }

  @Nullable
  private static Double roundToNearest(
      @Nullable Double samplingPercentage, boolean doNotLogWarnMessages) {
    if (samplingPercentage == null) {
      return null;
    }
    if (samplingPercentage == 0) {
      return 0.0;
    }
    double itemCount = 100 / samplingPercentage;
    double rounded = 100.0 / Math.round(itemCount);

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
