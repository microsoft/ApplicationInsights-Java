// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import ch.qos.logback.classic.LoggerContext;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentLogExporter;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LazyConfigurator {

  private static final Logger logger = LoggerFactory.getLogger(LazyConfigurator.class);

  private final TelemetryClient telemetryClient;
  private final AgentLogExporter agentLogExporter;
  private final AppIdSupplier appIdSupplier;

  LazyConfigurator(
      TelemetryClient telemetryClient,
      AgentLogExporter agentLogExporter,
      AppIdSupplier appIdSupplier) {
    this.telemetryClient = telemetryClient;
    this.agentLogExporter = agentLogExporter;
    this.appIdSupplier = appIdSupplier;
  }

  void updateConfiguration(LazyConfiguration config) {
    updateConnectionString(config.connectionString);
    updateRoleName(config.role.name);
    updateInstrumentationLoggingLevel(config.instrumentationLoggingLevel);
    updateSelfDiagnosticsLevel(config.selfDiagnosticsLevel);
  }

  private void updateConnectionString(@Nullable String connectionString) {
    if (!Strings.isNullOrEmpty(connectionString)) {
      TelemetryClient.getActive().updateConnectionStrings(connectionString, null, null);
      appIdSupplier.updateAppId();

      // now that we know the user has opted in to tracing, we need to init the propagator and
      // sampler
      DelegatingPropagator.getInstance().setUpStandardDelegate(Collections.emptyList(), false);
      // TODO handle APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE
      DelegatingSampler.getInstance().setAlwaysOnDelegate();
    }
  }

  private void updateRoleName(@Nullable String roleName) {
    if (!Strings.isNullOrEmpty(roleName)) {
      telemetryClient.updateRoleName(roleName);
    }
  }

  private void updateInstrumentationLoggingLevel(String instrumentationLoggingLevel) {
    if (instrumentationLoggingLevel != null) {
      agentLogExporter.setSeverityThreshold(
          Configuration.LoggingInstrumentation.getSeverityThreshold(instrumentationLoggingLevel));
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
