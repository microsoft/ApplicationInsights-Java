// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.configuration;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.SemanticAttributes;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.statsbeat.RpAttachType;
import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.Strings;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ConnectionStringOverride;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.InstrumentationKeyOverride;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.JmxMetric;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.SamplingOverride;
import com.microsoft.applicationinsights.agent.internal.diagnostics.DiagnosticsHelper;
import io.opentelemetry.api.common.AttributeKey;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
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

  public static final String APPLICATIONINSIGHTS_CONNECTION_STRING_ENV =
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
      "APPLICATIONINSIGHTS_SAMPLING_REQUESTS_PER_SECOND";

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

  private static final String APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_ENABLED =
      "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_ENABLED";

  // cannot use logger before loading configuration, so need to store warning messages locally until
  // logger is initialized
  private static final ConfigurationLogger configurationLogger = new ConfigurationLogger();
  public static final String CONFIGURATION_OPTIONS_LINK =
      "https://go.microsoft.com/fwlink/?linkid=2153358";

  // using deprecated fields to give warning message to user if they are still using them
  public static Configuration create(
      Path agentJarPath,
      @Nullable RpConfiguration rpConfiguration,
      Function<String, String> envVarsFunction,
      Function<String, String> systemPropsFunction)
      throws IOException {
    Configuration config =
        loadConfigurationFile(agentJarPath, envVarsFunction, systemPropsFunction);
    logConfigurationWarnings(config);
    overlayConfiguration(
        agentJarPath, rpConfiguration, config, envVarsFunction, systemPropsFunction);
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

    if (!config.preview.sampling.overrides.isEmpty()) {
      configurationLogger.warn(
          "\"Sampling overrides\" is no longer in preview and it has been GA since 3.5.0 GA,");
      config.sampling.overrides = config.preview.sampling.overrides;
    }
    for (SamplingOverride override : config.sampling.overrides) {
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
    if (config.preview.authentication.enabled && !config.authentication.enabled) {
      configurationLogger.warn(
          "\"authentication\" is no longer in preview and it has been GA since 3.4.18");
      config.authentication = config.preview.authentication;
    }
    if (config.authentication.clientSecret != null) {
      configurationLogger.warn(
          "\"clientsecret\" json configuration has been deprecated since 3.5.0 GA. Please use \"user-assigned managed identity\" or \"system-assigned managed identity\" instead. "
              + "If you're on premise, you can use APPLICATIONINSIGHTS_AUTHENTICATION_STRING environment variable to pass the client ID and secret, "
              + "e.g. APPLICATIONINSIGHTS_AUTHENTICATION_STRING=Authorization=AAD;ClientId={CLIENT_ID};ClientSecret={CLIENT_SECRET}.");
    }
    if (config.sampling.requestsPerSecond != null && config.sampling.percentage != null) {
      configurationLogger.warn(
          "Sampling \"requestsPerSecond\" and \"percentage\" should not be used at the same time."
              + " Please remove one of them.");
      config.sampling.percentage = null; // requestsPerSecond takes priority
    }

    logWarningIfUsingInternalAttributes(config);
  }

  private static void overlayConfiguration(
      Path agentJarPath,
      RpConfiguration rpConfiguration,
      Configuration config,
      Function<String, String> envVarsFunction,
      Function<String, String> systemPropsFunction)
      throws IOException {
    overlayFromEnv(config, agentJarPath.getParent(), envVarsFunction, systemPropsFunction);
    config.sampling.percentage = roundToNearest(config.sampling.percentage, true);
    for (SamplingOverride override : config.sampling.overrides) {
      supportSamplingOverridesOldSemConv(override);
      override.percentage = roundToNearest(override.percentage, true);
    }
    // rp configuration should always be last (so it takes precedence)
    // currently applicationinsights-rp.json is only used by Azure Spring Cloud
    if (rpConfiguration != null) {
      overlayFromEnv(rpConfiguration, envVarsFunction, systemPropsFunction);
      overlayRpConfiguration(config, rpConfiguration);
    }
    // only fall back to default sampling configuration after all overlays have been performed
    if (config.sampling.requestsPerSecond == null && config.sampling.percentage == null) {
      config.sampling.requestsPerSecond = 5.0;
      configurationLogger.info(
          "Some telemetry may be sampled out because a default sampling configuration was added in version 3.4.0 to reduce the default billing cost. You can set the sampling configuration explicitly: https://learn.microsoft.com/azure/azure-monitor/app/java-standalone-config#sampling");
    }
    supportTelemetryProcessorsOldSemConv(config);
  }

  private static void supportSamplingOverridesOldSemConv(SamplingOverride override) {
    for (Configuration.SamplingOverrideAttribute attribute : override.attributes) {
      attribute.key = mapAttributeKey(attribute.key);
    }
  }

  private static void supportTelemetryProcessorsOldSemConv(Configuration config) {
    for (Configuration.ProcessorConfig processor : config.preview.processors) {
      if (processor.include != null && processor.type == Configuration.ProcessorType.ATTRIBUTE) {
        for (Configuration.ProcessorAttribute attribute : processor.include.attributes) {
          attribute.key = mapAttributeKey(attribute.key);
        }
      }
      if (processor.exclude != null && processor.type == Configuration.ProcessorType.ATTRIBUTE) {
        for (Configuration.ProcessorAttribute attribute : processor.exclude.attributes) {
          attribute.key = mapAttributeKey(attribute.key);
        }
      }
      for (Configuration.ProcessorAction action : processor.actions) {
        if (action.key != null && processor.type == Configuration.ProcessorType.ATTRIBUTE) {
          action.key = AttributeKey.stringKey(mapAttributeKey(action.key.getKey()));
        }
      }
      if (processor.name != null && processor.name.fromAttributes != null) {
        List<String> newFromAttributes = new ArrayList<>();
        for (String oldFromAttribute : processor.name.fromAttributes) {
          String newFromAttribute = mapAttributeKey(oldFromAttribute);
          newFromAttributes.add(newFromAttribute);
        }
        processor.name.fromAttributes = newFromAttributes;
      }
      if (processor.body != null && processor.body.fromAttributes != null) {
        List<String> newFromAttributes = new ArrayList<>();
        for (String oldFromAttribute : processor.body.fromAttributes) {
          String newFromAttribute = mapAttributeKey(oldFromAttribute);
          newFromAttributes.add(newFromAttribute);
        }
        processor.body.fromAttributes = newFromAttributes;
      }
    }
  }

  private static String mapAttributeKey(String oldAttributeKey) {
    String result = null;
    // Common attributes across HTTP client and server spans
    if (oldAttributeKey.equals(SemanticAttributes.HTTP_METHOD.getKey())) {
      result = SemanticAttributes.HTTP_REQUEST_METHOD.getKey();
    } else if (oldAttributeKey.equals(SemanticAttributes.HTTP_STATUS_CODE.getKey())) {
      result = SemanticAttributes.HTTP_RESPONSE_STATUS_CODE.getKey();
    } else if (oldAttributeKey.startsWith("http.request.header.")
        || oldAttributeKey.startsWith("http.response.header.")) {
      result = oldAttributeKey.replace('_', '-');
    } else if (oldAttributeKey.equals(SemanticAttributes.NET_PROTOCOL_NAME.getKey())) {
      result = SemanticAttributes.NETWORK_PROTOCOL_NAME.getKey();
    } else if (oldAttributeKey.equals(SemanticAttributes.NET_PROTOCOL_VERSION.getKey())) {
      result = SemanticAttributes.NETWORK_PROTOCOL_VERSION.getKey();
    } else if (oldAttributeKey.equals(SemanticAttributes.NET_SOCK_PEER_ADDR.getKey())) {
      result = SemanticAttributes.NETWORK_PEER_ADDRESS.getKey();
    } else if (oldAttributeKey.equals(SemanticAttributes.NET_SOCK_PEER_PORT.getKey())) {
      result = SemanticAttributes.NETWORK_PEER_PORT.getKey();
    }

    // HTTP client span attributes
    // http.url is handled via LazyHttpUrl
    if (oldAttributeKey.equals(SemanticAttributes.HTTP_RESEND_COUNT.getKey())) {
      result = SemanticAttributes.HTTP_REQUEST_RESEND_COUNT.getKey();
      // becomes available.
    } else if (oldAttributeKey.equals(SemanticAttributes.NET_PEER_NAME.getKey())) {
      result = SemanticAttributes.SERVER_ADDRESS.getKey();
    } else if (oldAttributeKey.equals(SemanticAttributes.NET_PEER_PORT.getKey())) {
      result = SemanticAttributes.SERVER_PORT.getKey();
    }

    // HTTP server span attributes
    // http.target is handled via LazyHttpTarget
    if (oldAttributeKey.equals(SemanticAttributes.HTTP_SCHEME.getKey())) {
      result = SemanticAttributes.URL_SCHEME.getKey();
    } else if (oldAttributeKey.equals(SemanticAttributes.HTTP_CLIENT_IP.getKey())) {
      result = SemanticAttributes.CLIENT_ADDRESS.getKey();
    } else if (oldAttributeKey.equals(SemanticAttributes.NET_HOST_NAME.getKey())) {
      result = SemanticAttributes.SERVER_ADDRESS.getKey();
    } else if (oldAttributeKey.equals(SemanticAttributes.NET_HOST_PORT.getKey())) {
      result = SemanticAttributes.SERVER_PORT.getKey();
    }

    if (result == null) {
      result = oldAttributeKey;
    } else {
      configurationLogger.warn(
          "\"{}\" has been deprecated and replaced with \"{}\" since 3.5.0 GA.",
          oldAttributeKey,
          result);
    }
    return result;
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
    for (SamplingOverride override : config.sampling.overrides) {
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

  static void overlayProfilerEnvVars(
      Configuration config, Function<String, String> envVarsFunction) {
    String enabledString = Boolean.toString(config.preview.profiler.enabled);

    String overlayedValue =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_PREVIEW_PROFILER_ENABLED, enabledString, envVarsFunction);

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
                Boolean.toString(config.preview.profiler.enableDiagnostics),
                envVarsFunction));
  }

  private static boolean isOpenJ9Jvm() {
    String jvmName = System.getProperty("java.vm.name");
    return jvmName != null && jvmName.contains("OpenJ9");
  }

  private static void overlayAadEnvVars(
      Configuration config, Function<String, String> envVarsFunction) {
    String aadAuthString = getEnvVar(APPLICATIONINSIGHTS_AUTHENTICATION_STRING, envVarsFunction);
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
        config.authentication = new Configuration.AadAuthentication();
        config.authentication.enabled = true;
        config.authentication.type = Configuration.AuthenticationType.SAMI;
        String clientId = keyValueMap.get("ClientId");
        if (clientId != null && !clientId.isEmpty()) {
          config.authentication.clientId = clientId;
          String clientSecret = keyValueMap.get("ClientSecret");
          if (clientSecret != null && !clientSecret.isEmpty()) {
            // Override type to Client Secret
            config.authentication.type = Configuration.AuthenticationType.CLIENTSECRET;
            config.authentication.clientSecret = clientSecret;
          } else {
            // Override type to User Assigned Managed Identity
            config.authentication.type = Configuration.AuthenticationType.UAMI;
          }
        }
      }
    }
  }

  private static void loadLogCaptureEnvVar(
      Configuration config, Function<String, String> envVarsFunction) {
    String loggingEnabled =
        getEnvVar(APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_ENABLED, envVarsFunction);
    if (loggingEnabled != null) {
      configurationLogger.debug(
          "applying environment variable: {}={}",
          APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_ENABLED,
          loggingEnabled);
      config.instrumentation.logging.enabled = Boolean.parseBoolean(loggingEnabled);
    }
    if (config.instrumentation.logging.enabled) {
      String loggingLevel =
          getEnvVar(APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL, envVarsFunction);
      if (loggingLevel != null) {
        configurationLogger.debug(
            "applying environment variable: {}={}",
            APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL,
            loggingLevel);
        config.instrumentation.logging.level = loggingLevel;
      }
    }
  }

  // TODO deprecate this
  private static void loadJmxMetricsEnvVar(
      Configuration config, Function<String, String> envVarsFunction) throws IOException {
    String jmxMetricsEnvVarJson = getEnvVar(APPLICATIONINSIGHTS_JMX_METRICS, envVarsFunction);

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

  private static void overlayInstrumentationEnabledEnvVars(
      Configuration config, Function<String, String> envVarsFunction) {
    config.instrumentation.azureSdk.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_AZURE_SDK_ENABLED",
            config.instrumentation.azureSdk.enabled,
            envVarsFunction);
    config.instrumentation.cassandra.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_CASSANDRA_ENABLED",
            config.instrumentation.cassandra.enabled,
            envVarsFunction);
    config.instrumentation.jdbc.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_JDBC_ENABLED",
            config.instrumentation.jdbc.enabled,
            envVarsFunction);
    config.instrumentation.jms.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_JMS_ENABLED",
            config.instrumentation.jms.enabled,
            envVarsFunction);
    config.instrumentation.kafka.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_KAFKA_ENABLED",
            config.instrumentation.kafka.enabled,
            envVarsFunction);
    config.instrumentation.micrometer.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_MICROMETER_ENABLED",
            config.instrumentation.micrometer.enabled,
            envVarsFunction);
    config.instrumentation.mongo.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_MONGO_ENABLED",
            config.instrumentation.mongo.enabled,
            envVarsFunction);
    config.instrumentation.rabbitmq.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_RABBITMQ_ENABLED",
            config.instrumentation.rabbitmq.enabled,
            envVarsFunction);
    config.instrumentation.redis.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_REDIS_ENABLED",
            config.instrumentation.redis.enabled,
            envVarsFunction);
    config.instrumentation.springScheduling.enabled =
        overlayWithEnvVar(
            "APPLICATIONINSIGHTS_INSTRUMENTATION_SPRING_SCHEDULING_ENABLED",
            config.instrumentation.springScheduling.enabled,
            envVarsFunction);
  }

  private static Configuration loadConfigurationFile(
      Path agentJarPath,
      Function<String, String> envVarsFunction,
      Function<String, String> systemPropsFunction) {
    String configurationContent =
        getEnvVar(APPLICATIONINSIGHTS_CONFIGURATION_CONTENT, envVarsFunction);
    if (configurationContent != null) {
      return getConfigurationFromEnvVar(configurationContent, envVarsFunction);
    }

    Configuration configFromProperty =
        extractConfigFromProperty(agentJarPath, envVarsFunction, systemPropsFunction);
    if (configFromProperty != null) {
      return configFromProperty;
    }

    String runtimeAttachedConfigurationContent =
        getSystemProperty(
            APPLICATIONINSIGHTS_RUNTIME_ATTACHED_CONFIGURATION_CONTENT, systemPropsFunction);
    if (runtimeAttachedConfigurationContent != null) {
      return getConfiguration(runtimeAttachedConfigurationContent, JsonOrigin.RUNTIME_ATTACHED);
    }

    // only RP auto integrations do not support loading applicationinsights.json
    if (RpAttachType.getRpAttachType() == RpAttachType.INTEGRATED_AUTO) {
      // users do not have write access to agent directory in rp integrations
      // and rp integrations should not use applicationinsights.json because that makes it difficult
      // to merge rp intent and user intent
      return new Configuration();
    }

    Configuration configFromJsonNextToAgent = extractConfigFromJsonNextToAgentJar(agentJarPath);
    if (configFromJsonNextToAgent != null) {
      return configFromJsonNextToAgent;
    }

    if (getEnvVar("APPLICATIONINSIGHTS_PREVIEW_BSP_SCHEDULE_DELAY", System::getenv) != null) {
      // Note: OTEL_BSP_SCHEDULE_DELAY and OTEL_BLRP_SCHEDULE_DELAY could be used,
      // but should not be needed now that the default delay has been properly tuned
      configurationLogger.warn(
          "APPLICATIONINSIGHTS_PREVIEW_BSP_SCHEDULE_DELAY is no longer supported,"
              + " please report an issue to https://github.com/microsoft/ApplicationInsights-Java"
              + " if you are still in nead of this setting.");
    }

    // json configuration file is not required, ok to configure via env var alone
    return new Configuration();
  }

  @Nullable
  private static Configuration extractConfigFromProperty(
      Path agentJarPath,
      Function<String, String> envVarsFunction,
      Function<String, String> systemPropsFunction) {
    String configPathStr = getConfigPath(envVarsFunction, systemPropsFunction);
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
  static void overlayFromEnv(
      Configuration config,
      Path baseDir,
      Function<String, String> envVarsFunction,
      Function<String, String> systemPropertiesFunction)
      throws IOException {
    // load connection string from a file if connection string is in the format of
    // "${file:mounted_connection_string_file.txt}"
    Map<String, StringLookup> stringLookupMap =
        Collections.singletonMap(StringLookupFactory.KEY_FILE, new FileStringLookup(baseDir));
    StringLookup stringLookup =
        StringLookupFactory.INSTANCE.interpolatorStringLookup(stringLookupMap, null, false);
    StringSubstitutor stringSubstitutor = new StringSubstitutor(stringLookup);
    config.connectionString =
        overlayConnectionStringFromEnv(
            stringSubstitutor.replace(config.connectionString),
            envVarsFunction,
            systemPropertiesFunction);

    if (isTrimEmpty(config.role.name)) {
      // only use WEBSITE_SITE_NAME as a fallback
      config.role.name = getWebsiteSiteNameEnvVar(envVarsFunction);
    }
    config.role.name =
        overlayWithSysPropEnvVar(
            APPLICATIONINSIGHTS_ROLE_NAME_SYS,
            APPLICATIONINSIGHTS_ROLE_NAME_ENV,
            config.role.name,
            envVarsFunction,
            systemPropertiesFunction);

    if (isTrimEmpty(config.role.instance)) {
      // only use WEBSITE_INSTANCE_ID as a fallback
      config.role.instance = getEnvVar(WEBSITE_INSTANCE_ID, envVarsFunction);
    }
    config.role.instance =
        overlayWithSysPropEnvVar(
            APPLICATIONINSIGHTS_ROLE_INSTANCE_SYS,
            APPLICATIONINSIGHTS_ROLE_INSTANCE_ENV,
            config.role.instance,
            envVarsFunction,
            systemPropertiesFunction);

    config.sampling.percentage =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE, config.sampling.percentage, envVarsFunction);

    config.sampling.requestsPerSecond =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_SAMPLING_REQUESTS_PER_SECOND,
            config.sampling.requestsPerSecond,
            envVarsFunction);

    config.proxy = overlayProxyFromEnv(config.proxy, envVarsFunction);

    config.selfDiagnostics.level =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL,
            config.selfDiagnostics.level,
            envVarsFunction);
    config.selfDiagnostics.file.path =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_FILE_PATH,
            config.selfDiagnostics.file.path,
            envVarsFunction);

    String deprecatedMetricIntervalSeconds =
        getEnvVar(APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS, envVarsFunction);
    String metricIntervalSeconds =
        getEnvVar(APPLICATIONINSIGHTS_METRIC_INTERVAL_SECONDS, envVarsFunction);
    if (metricIntervalSeconds != null) {
      config.metricIntervalSeconds =
          overlayWithEnvVar(
              APPLICATIONINSIGHTS_METRIC_INTERVAL_SECONDS,
              config.metricIntervalSeconds,
              envVarsFunction);
    } else if (deprecatedMetricIntervalSeconds != null) {
      configurationLogger.warn(
          "\"APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS\" has been renamed to \"APPLICATIONINSIGHTS_METRIC_INTERVAL_SECONDS\""
              + " in 3.4.9 (GA)");
      config.metricIntervalSeconds =
          overlayWithEnvVar(
              APPLICATIONINSIGHTS_PREVIEW_METRIC_INTERVAL_SECONDS,
              config.metricIntervalSeconds,
              envVarsFunction);
    }

    config.preview.instrumentation.springIntegration.enabled =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_PREVIEW_INSTRUMENTATION_SPRING_INTEGRATION_ENABLED,
            config.preview.instrumentation.springIntegration.enabled,
            envVarsFunction);

    config.preview.liveMetrics.enabled =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_PREVIEW_LIVE_METRICS_ENABLED,
            config.preview.liveMetrics.enabled,
            envVarsFunction);

    config.preview.statsbeat.disabled =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_STATSBEAT_DISABLED,
            config.preview.statsbeat.disabled,
            envVarsFunction);

    loadLogCaptureEnvVar(config, envVarsFunction);
    loadJmxMetricsEnvVar(config, envVarsFunction);

    overlayProfilerEnvVars(config, envVarsFunction);
    overlayAadEnvVars(config, envVarsFunction);
    overlayInstrumentationEnabledEnvVars(config, envVarsFunction);
  }

  public static void overlayFromEnv(
      RpConfiguration config,
      Function<String, String> envVarsFunction,
      Function<String, String> systemPropertiesFunction) {
    config.connectionString =
        overlayConnectionStringFromEnv(
            config.connectionString, envVarsFunction, systemPropertiesFunction);
    config.sampling.percentage =
        overlayWithEnvVar(
            APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE, config.sampling.percentage, envVarsFunction);
  }

  @Nullable
  private static String overlayConnectionStringFromEnv(
      String connectionString,
      Function<String, String> envVarsFunction,
      Function<String, String> systemPropertiesFunction) {
    String value =
        overlayWithSysPropEnvVar(
            APPLICATIONINSIGHTS_CONNECTION_STRING_SYS,
            APPLICATIONINSIGHTS_CONNECTION_STRING_ENV,
            connectionString,
            envVarsFunction,
            systemPropertiesFunction);

    if (value != null) {
      return value;
    }

    // this is for backwards compatibility only
    String instrumentationKey = getEnvVar(APPINSIGHTS_INSTRUMENTATIONKEY, envVarsFunction);
    if (instrumentationKey != null) {
      configurationLogger.warn(
          "APPINSIGHTS_INSTRUMENTATIONKEY is only supported for backwards compatibility,"
              + " please consider using APPLICATIONINSIGHTS_CONNECTION_STRING instead");
      return "InstrumentationKey=" + instrumentationKey;
    }

    return null;
  }

  private static Configuration.Proxy overlayProxyFromEnv(
      Configuration.Proxy proxy, Function<String, String> envVarsFunction) {
    String proxyEnvVar = getEnvVar(APPLICATIONINSIGHTS_PROXY, envVarsFunction);
    if (proxyEnvVar == null) {
      if (proxy.password != null) {
        configurationLogger.warn(
            "Storing the proxy password in the application insights json configuration file"
                + " is deprecated because it is not secure. Please use the"
                + " APPLICATIONINSIGHTS_PROXY environment variable instead which supports passing"
                + " the username and password, e.g."
                + " APPLICATIONINSIGHTS_PROXY=https://myuser:mypassword@myproxy:8888");
        if (proxy.host != null
            && (proxy.host.startsWith("http://") || proxy.host.startsWith("https://"))) {
          throw new FriendlyException(
              "The proxy host should not start with http:// or https://",
              "Please remove http:// or https:// from the proxy host.");
        }
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

  private static String getConfigPath(
      Function<String, String> envVarsFunction, Function<String, String> systemPropertiesFunction) {
    String configPath = getEnvVar(APPLICATIONINSIGHTS_CONFIGURATION_FILE, envVarsFunction);
    if (configPath != null) {
      return configPath;
    }
    // intentionally not checking system properties for other system properties
    // with the intention to keep configuration paths minimal to help with supportability
    return getSystemProperty("applicationinsights.configuration.file", systemPropertiesFunction);
  }

  private static String getWebsiteSiteNameEnvVar(Function<String, String> envVarsFunction) {
    String websiteSiteName = getEnvVar(WEBSITE_SITE_NAME, envVarsFunction);
    if (websiteSiteName != null && inAzureFunctionsWorker(envVarsFunction)) {
      // special case for Azure Functions
      return websiteSiteName.toLowerCase(Locale.ROOT);
    }
    return websiteSiteName;
  }

  public static boolean inAzureFunctionsConsumptionWorker() {
    // for now its the same, but in future should be different check
    return inAzureFunctionsWorker(System::getenv);
  }

  public static boolean inAzureFunctionsWorker(Function<String, String> envVarsFunction) {
    // supporting both Azure Functions RP Integration, as well as bring your own agent deployments
    // in Azure Functions
    return "java".equals(envVarsFunction.apply("FUNCTIONS_WORKER_RUNTIME"));
  }

  public static String overlayWithSysPropEnvVar(
      String systemPropertyName,
      String envVarName,
      String defaultValue,
      Function<String, String> envVarsFunction,
      Function<String, String> systemPropertiesFunction) {
    String value = getSystemProperty(systemPropertyName, systemPropertiesFunction);
    if (value != null) {
      configurationLogger.debug("using system property: {}", systemPropertyName);
      return value;
    }
    return overlayWithEnvVar(envVarName, defaultValue, envVarsFunction);
  }

  public static String overlayWithEnvVar(
      String name, String defaultValue, Function<String, String> envVarsFunction) {
    String value = getEnvVar(name, envVarsFunction);
    if (value != null) {
      return value;
    }
    return defaultValue;
  }

  @Nullable
  static Double overlayWithEnvVar(
      String name, @Nullable Double defaultValue, Function<String, String> envVarsFunction) {
    String value = getEnvVar(name, envVarsFunction);
    if (value != null) {
      configurationLogger.debug("applying environment variable: {}={}", name, value);
      // intentionally allowing NumberFormatException to bubble up as invalid configuration and
      // prevent agent from starting
      return Double.parseDouble(value);
    }
    return defaultValue;
  }

  static int overlayWithEnvVar(
      String name, int defaultValue, Function<String, String> envVarsFunction) {
    String value = getEnvVar(name, envVarsFunction);
    if (value != null) {
      configurationLogger.debug("using environment variable: {}", name);
      // intentionally allowing NumberFormatException to bubble up as invalid configuration and
      // prevent agent from starting
      return Integer.parseInt(value);
    }
    return defaultValue;
  }

  static boolean overlayWithEnvVar(
      String name, boolean defaultValue, Function<String, String> envVarsFunction) {
    String value = getEnvVar(name, envVarsFunction);
    if (value != null) {
      configurationLogger.debug("applying environment variable: {}={}", name, value);
      return Boolean.parseBoolean(value);
    }
    return defaultValue;
  }

  // never returns empty string (empty string is normalized to null)
  protected static String getSystemProperty(
      String name, @Nullable Function<String, String> systemPropertiesFunction) {
    if (systemPropertiesFunction == null) {
      systemPropertiesFunction = System::getProperty;
    }
    String value = Strings.trimAndEmptyToNull(systemPropertiesFunction.apply(name));
    if (value != null) {
      configurationLogger.debug("read system property: {}={}", name, value);
    }
    return value;
  }

  // never returns empty string (empty string is normalized to null)
  protected static String getEnvVar(
      String name, @Nullable Function<String, String> envVarsFunction) {
    if (envVarsFunction == null) {
      envVarsFunction = System::getenv;
    }
    String value = Strings.trimAndEmptyToNull(envVarsFunction.apply(name));
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

  static Configuration getConfigurationFromEnvVar(
      String json, Function<String, String> envVarsFunction) {

    Configuration configuration = getConfiguration(json, JsonOrigin.ENV_VAR);

    // restrict connection string in APPLICATIONINSIGHTS_CONFIGURATION_CONTENT for App Service
    // INTEGRATED_AUTO only
    if (configuration.connectionString != null && DiagnosticsHelper.isAppSvcRpIntegratedAuto()) {
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
