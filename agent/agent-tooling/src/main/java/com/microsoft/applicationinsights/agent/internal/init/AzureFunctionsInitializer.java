// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import ch.qos.logback.classic.LoggerContext;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.AgentLogExporter;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.javaagent.bootstrap.ClassFileTransformerHolder;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureFunctionsInitializer implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(AzureFunctionsInitializer.class);

  private static final Logger diagnosticLogger =
      LoggerFactory.getLogger(DiagnosticsHelper.DIAGNOSTICS_LOGGER_NAME);

  private final TelemetryClient telemetryClient;
  private final AgentLogExporter agentLogExporter;
  private final AppIdSupplier appIdSupplier;

  public AzureFunctionsInitializer(
      TelemetryClient telemetryClient,
      AgentLogExporter agentLogExporter,
      AppIdSupplier appIdSupplier) {
    this.telemetryClient = telemetryClient;
    this.agentLogExporter = agentLogExporter;
    this.appIdSupplier = appIdSupplier;
  }

  @Override
  public void run() {
    if (!isAgentEnabled()) {
      try {
        disableBytecodeInstrumentation();
        diagnosticLogger.info("Application Insights Java Agent disabled");
      } catch (Throwable t) {
        diagnosticLogger.error(
            "Application Insights Java Agent disablement failed: " + t.getMessage(), t);
      }
      return;
    }
    try {
      initialize();
      diagnosticLogger.info("Application Insights Java Agent specialized successfully");
    } catch (Throwable t) {
      diagnosticLogger.error(
          "Application Insights Java Agent specialization failed: " + t.getMessage(), t);
    }
  }

  private void initialize() {
    String selfDiagnosticsLevel = System.getenv("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL");
    String connectionString = System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING");
    String instrumentationKey = System.getenv("APPINSIGHTS_INSTRUMENTATIONKEY");
    String websiteSiteName = System.getenv("WEBSITE_SITE_NAME");
    String instrumentationLoggingLevel =
        System.getenv("APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL");

    logger.debug("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL: {}", selfDiagnosticsLevel);
    logger.debug("APPLICATIONINSIGHTS_CONNECTION_STRING: {}", connectionString);
    if (Strings.isNullOrEmpty(connectionString)) {
      logger.debug("APPINSIGHTS_INSTRUMENTATIONKEY: {}", instrumentationKey);
    }
    logger.debug("WEBSITE_SITE_NAME: {}", websiteSiteName);
    logger.debug(
        "APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL: {}", instrumentationLoggingLevel);

    setConnectionString(connectionString, instrumentationKey);
    setWebsiteSiteName(websiteSiteName);
    setSelfDiagnosticsLevel(selfDiagnosticsLevel);
    if (instrumentationLoggingLevel != null) {
      agentLogExporter.setSeverityThreshold(
          Configuration.LoggingInstrumentation.getSeverityThreshold(instrumentationLoggingLevel));
    }
  }

  private static void disableBytecodeInstrumentation() {
    Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();
    ClassFileTransformer transformer = ClassFileTransformerHolder.getClassFileTransformer();
    if (instrumentation == null || transformer == null) {
      return;
    }
    if (instrumentation.removeTransformer(transformer)) {
      ClassFileTransformerHolder.setClassFileTransformer(null);
    }
  }

  // visible for testing
  void setConnectionString(@Nullable String connectionString, @Nullable String instrumentationKey) {
    if (connectionString != null && !connectionString.isEmpty()) {
      setValue(connectionString);
    } else {
      // if the instrumentation key is neither null nor empty , we will create a default
      // connection string based on the instrumentation key.
      // this is to support Azure Functions that were created prior to the introduction of
      // connection strings
      if (instrumentationKey != null && !instrumentationKey.isEmpty()) {
        setValue("InstrumentationKey=" + instrumentationKey);
      }
    }
  }

  private void setValue(String value) {
    telemetryClient.updateConnectionStrings(value, null, null);
    appIdSupplier.updateAppId();

    // now that we know the user has opted in to tracing, we need to init the propagator and sampler
    DelegatingPropagator.getInstance().setUpStandardDelegate(Collections.emptyList(), false);
    // TODO handle APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE
    DelegatingSampler.getInstance().setAlwaysOnDelegate();
  }

  void setWebsiteSiteName(@Nullable String websiteSiteName) {
    if (websiteSiteName != null && !websiteSiteName.isEmpty()) {
      telemetryClient.updateRoleName(websiteSiteName);
    }
  }

  static void setSelfDiagnosticsLevel(@Nullable String loggingLevel) {
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

  static boolean isAgentEnabled() {
    String enableAgent = System.getenv("APPLICATIONINSIGHTS_ENABLE_AGENT");
    boolean enableAgentDefault = Boolean.getBoolean("LazySetOptIn");
    logger.debug("APPLICATIONINSIGHTS_ENABLE_AGENT: {}", enableAgent);
    logger.debug("LazySetOptIn: {}", enableAgentDefault);
    return isAgentEnabled(enableAgent, enableAgentDefault);
  }

  // visible for tests
  static boolean isAgentEnabled(@Nullable String enableAgent, boolean defaultValue) {
    if (enableAgent == null) {
      // APPLICATIONINSIGHTS_ENABLE_AGENT is not set, use the default value (LazySetOptIn)
      return defaultValue;
    }
    return Boolean.parseBoolean(enableAgent);
  }
}
