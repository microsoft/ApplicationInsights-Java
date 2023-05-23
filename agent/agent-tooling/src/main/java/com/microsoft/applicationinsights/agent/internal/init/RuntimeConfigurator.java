// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import static java.util.concurrent.TimeUnit.MINUTES;

import ch.qos.logback.classic.LoggerContext;
import com.azure.monitor.opentelemetry.exporter.implementation.heartbeat.HeartbeatExporter;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.internal.classicsdk.BytecodeUtilImpl;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentLogExporter;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.profiler.ProfilingInitializer;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeConfigurator {

  private static final Logger logger = LoggerFactory.getLogger(RuntimeConfigurator.class);

  private final TelemetryClient telemetryClient;
  private final Supplier<AgentLogExporter> agentLogExporter;
  private final Configuration initialConfig;
  private volatile RuntimeConfiguration currentConfig;
  private final Consumer<List<TelemetryItem>> heartbeatTelemetryItemsConsumer;
  private final File tempDir;

  private final AtomicBoolean profilerStarted = new AtomicBoolean();
  private final AtomicBoolean heartbeatStarted = new AtomicBoolean();

  RuntimeConfigurator(
      TelemetryClient telemetryClient,
      Supplier<AgentLogExporter> agentLogExporter,
      Configuration initialConfig,
      Consumer<List<TelemetryItem>> heartbeatTelemetryItemConsumer,
      File tempDir) {
    this.telemetryClient = telemetryClient;
    this.agentLogExporter = agentLogExporter;
    this.initialConfig = initialConfig;
    currentConfig = captureInitialConfig(initialConfig);
    this.heartbeatTelemetryItemsConsumer = heartbeatTelemetryItemConsumer;
    this.tempDir = tempDir;
  }

  private static RuntimeConfiguration captureInitialConfig(Configuration initialConfig) {
    RuntimeConfiguration runtimeConfig = new RuntimeConfiguration();
    runtimeConfig.connectionString = initialConfig.connectionString;
    runtimeConfig.role.name = initialConfig.role.name;
    runtimeConfig.role.instance = initialConfig.role.instance;

    runtimeConfig.sampling.percentage = initialConfig.sampling.percentage;
    runtimeConfig.sampling.requestsPerSecond = initialConfig.sampling.requestsPerSecond;
    runtimeConfig.samplingPreview.parentBased = initialConfig.preview.sampling.parentBased;
    // TODO (trask) make deep copies? (not needed currently)
    runtimeConfig.samplingPreview.overrides =
        new ArrayList<>(initialConfig.preview.sampling.overrides);

    runtimeConfig.propagationDisabled = initialConfig.preview.disablePropagation;
    runtimeConfig.additionalPropagators =
        new ArrayList<>(initialConfig.preview.additionalPropagators);
    runtimeConfig.legacyRequestIdPropagationEnabled =
        initialConfig.preview.legacyRequestIdPropagation.enabled;

    runtimeConfig.instrumentationLoggingLevel = initialConfig.instrumentation.logging.level;
    runtimeConfig.selfDiagnosticsLevel = initialConfig.selfDiagnostics.level;

    runtimeConfig.profilerEnabled = initialConfig.preview.profiler.enabled;
    runtimeConfig.heartbeatIntervalSeconds = initialConfig.heartbeat.intervalSeconds;
    return runtimeConfig;
  }

  private static RuntimeConfiguration copy(RuntimeConfiguration config) {
    RuntimeConfiguration copy = new RuntimeConfiguration();
    copy.connectionString = config.connectionString;
    copy.role.name = config.role.name;
    copy.role.instance = config.role.instance;

    copy.sampling.percentage = config.sampling.percentage;
    copy.sampling.requestsPerSecond = config.sampling.requestsPerSecond;
    copy.samplingPreview.parentBased = config.samplingPreview.parentBased;
    // TODO (trask) make deep copies? (not needed currently)
    copy.samplingPreview.overrides = new ArrayList<>(config.samplingPreview.overrides);

    copy.propagationDisabled = config.propagationDisabled;
    copy.additionalPropagators = new ArrayList<>(config.additionalPropagators);
    copy.legacyRequestIdPropagationEnabled = config.legacyRequestIdPropagationEnabled;

    copy.instrumentationLoggingLevel = config.instrumentationLoggingLevel;
    copy.selfDiagnosticsLevel = config.selfDiagnosticsLevel;

    copy.profilerEnabled = config.profilerEnabled;
    copy.heartbeatIntervalSeconds = config.heartbeatIntervalSeconds;
    return copy;
  }

  public RuntimeConfiguration getCurrentConfigCopy() {
    return copy(currentConfig);
  }

  public void apply(RuntimeConfiguration runtimeConfig) {

    logger.debug("Applying runtime configuration");

    boolean enabled = !Strings.isNullOrEmpty(runtimeConfig.connectionString);
    boolean currentEnabled = !Strings.isNullOrEmpty(currentConfig.connectionString);

    updateConnectionString(runtimeConfig.connectionString);
    updateRoleName(runtimeConfig.role.name);
    updateRoleInstance(runtimeConfig.role.instance);

    // ok to update propagation if it hasn't changed
    updatePropagation(
        !runtimeConfig.propagationDisabled && enabled,
        runtimeConfig.additionalPropagators,
        runtimeConfig.legacyRequestIdPropagationEnabled);

    // don't update sampling if it hasn't changed, since that will wipe out state of any
    // rate-limited samplers
    if (enabled != currentEnabled
        || !Objects.equals(runtimeConfig.sampling.percentage, currentConfig.sampling.percentage)
        || !Objects.equals(
            runtimeConfig.sampling.requestsPerSecond, currentConfig.sampling.requestsPerSecond)) {
      updateSampling(enabled, runtimeConfig.sampling, runtimeConfig.samplingPreview);
    }

    // initialize Profiler
    if (runtimeConfig.profilerEnabled && telemetryClient.getConnectionString() != null) {
      // this prevents profiler being initialized more than once in Azure Spring App
      if (!profilerStarted.getAndSet(true)) {
        try {
          ProfilingInitializer.initialize(
              tempDir,
              initialConfig.preview.profiler,
              initialConfig.preview.gcEvents.reportingLevel,
              runtimeConfig.role.name,
              runtimeConfig.role.instance,
              telemetryClient);
        } catch (RuntimeException e) {
          logger.warn("Failed to initialize profiler", e);
        }
      } else {
        logger.debug("Profiler has already been initialized.");
      }
    }

    // enable Heartbeat
    if (telemetryClient.getConnectionString() != null) {
      // this prevents heartbeat being started more than once in Azure Spring App
      if (!heartbeatStarted.getAndSet(true)) {
        // interval longer than 15 minutes is not allowed since we use this data for usage telemetry
        long intervalSeconds =
            Math.min(runtimeConfig.heartbeatIntervalSeconds, MINUTES.toSeconds(15));
        HeartbeatExporter.start(
            intervalSeconds, telemetryClient::populateDefaults, heartbeatTelemetryItemsConsumer);
      } else {
        logger.debug("Heartbeat has already started.");
      }
    }

    // TODO (heya) enable Statsbeat and need to refactor RuntimeConfiguration

    updateInstrumentationLoggingLevel(runtimeConfig.instrumentationLoggingLevel);
    updateSelfDiagnosticsLevel(runtimeConfig.selfDiagnosticsLevel);

    currentConfig = runtimeConfig;
  }

  static void updatePropagation(
      boolean enabled,
      List<String> additionalPropagators,
      boolean legacyRequestIdPropagationEnabled) {

    if (enabled) {
      DelegatingPropagator.getInstance()
          .setUpStandardDelegate(additionalPropagators, legacyRequestIdPropagationEnabled);
    } else {
      DelegatingPropagator.getInstance().reset();
    }
  }

  static void updateSampling(
      boolean enabled,
      Configuration.Sampling sampling,
      Configuration.SamplingPreview samplingPreview) {

    if (!enabled) {
      DelegatingSampler.getInstance().reset();
      BytecodeUtilImpl.samplingPercentage = 0;
      return;
    }

    DelegatingSampler.getInstance().setDelegate(Samplers.getSampler(sampling, samplingPreview));
    if (sampling.percentage != null) {
      BytecodeUtilImpl.samplingPercentage = sampling.percentage.floatValue();
    } else {
      BytecodeUtilImpl.samplingPercentage = 100;
    }
  }

  private void updateConnectionString(@Nullable String connectionString) {
    telemetryClient.updateConnectionStrings(connectionString, null, null);
  }

  private void updateRoleName(@Nullable String roleName) {
    if (!Strings.isNullOrEmpty(roleName)) {
      telemetryClient.updateRoleName(roleName);
    }
  }

  private void updateRoleInstance(@Nullable String roleInstance) {
    if (!Strings.isNullOrEmpty(roleInstance)) {
      telemetryClient.updateRoleInstance(roleInstance);
    }
  }

  private void updateInstrumentationLoggingLevel(String instrumentationLoggingLevel) {
    if (instrumentationLoggingLevel != null) {
      AgentLogExporter exporter = agentLogExporter.get();
      if (exporter != null) {
        exporter.setSeverityThreshold(
            Configuration.LoggingInstrumentation.getSeverityThreshold(instrumentationLoggingLevel));
      }
    }
  }

  private static void updateSelfDiagnosticsLevel(@Nullable String loggingLevel) {
    if (loggingLevel == null || !loggingLevel.isEmpty()) {
      return;
    }

    LoggingLevelConfigurator configurator;
    try {
      configurator = new LoggingLevelConfigurator(loggingLevel);
    } catch (IllegalArgumentException exception) {
      logger.warn("unexpected self-diagnostic level: {}", loggingLevel);
      return;
    }

    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    configurator.initLoggerLevels(loggerContext);

    // also need to update any previously created loggers
    List<ch.qos.logback.classic.Logger> loggerList = loggerContext.getLoggerList();
    loggerList.forEach(configurator::updateLoggerLevel);
  }
}
