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
import com.microsoft.applicationinsights.agent.internal.legacyheaders.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.wascore.TelemetryClient;
import io.opentelemetry.instrumentation.api.aisdk.AiLazyConfiguration;
import io.opentelemetry.instrumentation.api.config.Config;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyConfigurationAccessor implements AiLazyConfiguration.Accessor {

  private static final Logger logger = LoggerFactory.getLogger(LazyConfigurationAccessor.class);

  private final TelemetryClient telemetryClient;
  private final AppIdSupplier appIdSupplier;

  public LazyConfigurationAccessor(TelemetryClient telemetryClient, AppIdSupplier appIdSupplier) {
    this.telemetryClient = telemetryClient;
    this.appIdSupplier = appIdSupplier;
  }

  @Override
  public void lazyLoad() {
    String instrumentationKey = telemetryClient.getInstrumentationKey();
    String roleName = telemetryClient.getRoleName();
    if (instrumentationKey != null
        && !instrumentationKey.isEmpty()
        && roleName != null
        && !roleName.isEmpty()) {
      return;
    }

    boolean lazySetOptIn = Boolean.parseBoolean(System.getProperty("LazySetOptIn"));
    String enableAgent = System.getenv("APPLICATIONINSIGHTS_ENABLE_AGENT");
    logger.debug("lazySetOptIn: {}", lazySetOptIn);
    logger.debug("APPLICATIONINSIGHTS_ENABLE_AGENT: {}", enableAgent);
    if (!shouldSetConnectionString(lazySetOptIn, enableAgent)) {
      return;
    }

    setConnectionString(
        System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING"),
        System.getenv("APPINSIGHTS_INSTRUMENTATIONKEY"));
    setWebsiteSiteName(System.getenv("WEBSITE_SITE_NAME"));
    setSelfDiagnosticsLevel(System.getenv("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL"));
    setInstrumentationLoggingLevel(
        System.getenv("APPLICATIONINSIGHTS_INSTRUMENTATION_LOGGING_LEVEL"));
  }

  void setConnectionString(String connectionString, String instrumentationKey) {
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
    telemetryClient.setConnectionString(value);
    // now that we know the user has opted in to tracing, we need to init the legacyheaders and
    // sampler
    DelegatingPropagator.getInstance().setUpStandardDelegate();
    // TODO handle APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE
    DelegatingSampler.getInstance().setAlwaysOnDelegate();
    logger.info("Set connection string {} lazily for the Azure Function Consumption Plan.", value);

    // register and start app id retrieval after the connection string becomes available.
    appIdSupplier.registerAndStartAppIdRetrieval();
  }

  void setWebsiteSiteName(String websiteSiteName) {
    if (websiteSiteName != null && !websiteSiteName.isEmpty()) {
      telemetryClient.setRoleName(websiteSiteName);
      logger.info(
          "Set WEBSITE_SITE_NAME: {} lazily for the Azure Function Consumption Plan.",
          websiteSiteName);
    }
  }

  static void setSelfDiagnosticsLevel(String loggingLevel) {
    if (loggingLevel == null || !loggingLevel.isEmpty()) {
      return;
    }
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    List<ch.qos.logback.classic.Logger> loggerList = loggerContext.getLoggerList();
    logger.info("setting APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL to {}", loggingLevel);
    LoggingLevelConfigurator configurator;
    try {
      configurator = new LoggingLevelConfigurator(loggingLevel);
    } catch (IllegalArgumentException exception) {
      logger.warn("unexpected self-diagnostic level: {}", loggingLevel);
      return;
    }
    loggerList.forEach(configurator::updateLoggerLevel);
    logger.debug("self-diagnostics logging level has been updated.");
  }

  static boolean shouldSetConnectionString(boolean lazySetOptIn, String enableAgent) {
    if (lazySetOptIn) {
      // when LazySetOptIn is on, enable agent if APPLICATIONINSIGHTS_ENABLE_AGENT is null or true
      if (enableAgent == null || Boolean.parseBoolean(enableAgent)) {
        return true;
      }
    } else {
      // when LazySetOptIn is off, enable agent if APPLICATIONINSIGHTS_ENABLE_AGENT is true
      if (Boolean.parseBoolean(enableAgent)) {
        return true;
      }
    }
    return false;
  }

  static void setInstrumentationLoggingLevel(String loggingLevel) {
    if (loggingLevel != null && !loggingLevel.isEmpty()) {
      Config.get()
          .updateProperty("otel.experimental.log.capture.threshold", loggingLevel.toUpperCase());
    }
  }
}
