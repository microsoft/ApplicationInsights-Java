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

package com.microsoft.applicationinsights.agent.internal.init;

import ch.qos.logback.classic.LoggerContext;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.ConnectionString;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.StatsbeatConnectionString;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
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

  private static final Logger startupLogger =
      LoggerFactory.getLogger("com.microsoft.applicationinsights.agent");

  private static final Logger logger = LoggerFactory.getLogger(AzureFunctionsInitializer.class);

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
      disableBytecodeInstrumentation();
      return;
    }

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
    agentLogExporter.setThreshold(
        Configuration.LoggingInstrumentation.getSeverity(instrumentationLoggingLevel));

    startupLogger.info(
        "ApplicationInsights Java Agent specialization complete for Azure Functions placeholder");
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
    ConnectionString connectionString = ConnectionString.parse(value);
    telemetryClient.updateConnectionString(connectionString);
    telemetryClient.updateStatsbeatConnectionString(
        StatsbeatConnectionString.create(connectionString, null, null));

    // now that we know the user has opted in to tracing, we need to init the propagator and sampler
    DelegatingPropagator.getInstance().setUpStandardDelegate(Collections.emptyList(), false);
    // TODO handle APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE
    DelegatingSampler.getInstance().setAlwaysOnDelegate();

    // start app id retrieval after the connection string becomes available.
    appIdSupplier.startAppIdRetrieval();
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

  // since the agent is already running at this point, this really just determines whether the
  // telemetry is sent to the ingestion service or not (essentially behaving to the user as if the
  // agent is not enabled)
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
