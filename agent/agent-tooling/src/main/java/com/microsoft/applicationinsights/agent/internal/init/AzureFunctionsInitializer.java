// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.autoconfigure.implementation.utils.Strings;
import com.microsoft.applicationinsights.agent.internal.diagnostics.DiagnosticsHelper;
import io.opentelemetry.javaagent.bootstrap.InstrumentationHolder;
import io.opentelemetry.javaagent.bootstrap.LambdaTransformer;
import io.opentelemetry.javaagent.bootstrap.LambdaTransformerHolder;
import io.opentelemetry.javaagent.tooling.Java8LambdaTransformer;
import io.opentelemetry.javaagent.tooling.Java9LambdaTransformer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureFunctionsInitializer implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(AzureFunctionsInitializer.class);

  private static final Logger diagnosticLogger =
      LoggerFactory.getLogger(DiagnosticsHelper.DIAGNOSTICS_LOGGER_NAME);

  private final RuntimeConfigurator runtimeConfigurator;

  public AzureFunctionsInitializer(RuntimeConfigurator runtimeConfigurator) {
    this.runtimeConfigurator = runtimeConfigurator;
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

  private static void disableBytecodeInstrumentation() {
    Instrumentation instrumentation = InstrumentationHolder.getInstrumentation();
    LambdaTransformer transformer = LambdaTransformerHolder.getLambdaTransformer();
    ClassFileTransformer classFileTransformer = getDelegate(transformer);
    if (instrumentation == null || classFileTransformer == null) {
      return;
    }
    if (instrumentation.removeTransformer(classFileTransformer)) {
      LambdaTransformerHolder.setLambdaTransformer(null);
    }
  }

  @Nullable
  private static ClassFileTransformer getDelegate(LambdaTransformer transformer) {
    if (transformer instanceof Java8LambdaTransformer) {
      try {
        return (ClassFileTransformer)
            Java8LambdaTransformer.class.getDeclaredField("delegate").get(transformer);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(e);
      }
    }
    if (transformer instanceof Java9LambdaTransformer) {
      try {
        return (ClassFileTransformer)
            Java9LambdaTransformer.class.getDeclaredField("delegate").get(transformer);
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(e);
      }
    }
    throw new IllegalStateException(
        "Unexpected LambdaTransformer implementation: " + transformer.getClass());
  }

  private void initialize() {
    RuntimeConfiguration runtimeConfig = runtimeConfigurator.getCurrentConfigCopy();

    runtimeConfig.connectionString = getAndLogAtDebug("APPLICATIONINSIGHTS_CONNECTION_STRING");
    if (runtimeConfig.connectionString == null) {
      // if the instrumentation key is neither null nor empty, we build the connection string based
      // on APPINSIGHTS_INSTRUMENTATIONKEY.
      // this is to support Azure Functions that were created prior to the introduction of
      // connection strings
      String instrumentationKey = getAndLogAtDebug("APPINSIGHTS_INSTRUMENTATIONKEY");
      if (!Strings.isNullOrEmpty(instrumentationKey)) {
        runtimeConfig.connectionString = "InstrumentationKey=" + instrumentationKey;
      }
    }
    runtimeConfig.role.name = getAndLogAtDebug("WEBSITE_SITE_NAME");
    runtimeConfig.instrumentationLoggingLevel =
        getAndLogAtDebug("APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL");
    runtimeConfig.selfDiagnosticsLevel =
        getAndLogAtDebug("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL");

    runtimeConfigurator.apply(runtimeConfig);
  }

  static boolean isAgentEnabled() {
    String enableAgent = getAndLogAtDebug("APPLICATIONINSIGHTS_ENABLE_AGENT");
    boolean enableAgentDefault = Boolean.getBoolean("LazySetOptIn");
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

  @Nullable
  private static String getAndLogAtDebug(String envVarName) {
    String value = Strings.trimAndEmptyToNull(System.getenv(envVarName));
    if (value != null) {
      logger.debug("read environment variable: {}={}", envVarName, value);
    }
    return value;
  }
}
