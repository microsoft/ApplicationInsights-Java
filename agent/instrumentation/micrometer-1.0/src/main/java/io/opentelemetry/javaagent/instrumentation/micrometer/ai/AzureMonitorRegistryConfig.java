// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.api.incubator.config.internal.DeclarativeConfigUtil;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class AzureMonitorRegistryConfig implements StepRegistryConfig {

  private static final Logger logger = Logger.getLogger(AzureMonitorRegistryConfig.class.getName());
  private static final Duration DEFAULT_STEP = Duration.ofSeconds(60);

  private final Duration step;
  @Nullable private final String namespace;

  public static final AzureMonitorRegistryConfig INSTANCE = new AzureMonitorRegistryConfig();

  private AzureMonitorRegistryConfig() {
    DeclarativeConfigProperties config =
        DeclarativeConfigUtil.getInstrumentationConfig(GlobalOpenTelemetry.get(), "micrometer");

    // Get step duration in milliseconds, default to 60 seconds
    String stepMillisStr = config.getString("applicationinsights.internal.micrometer.step.millis");
    // Fallback to system property if not found in declarative config
    if (stepMillisStr == null) {
      stepMillisStr = System.getProperty("applicationinsights.internal.micrometer.step.millis");
    }
    
    Duration parsedStep = DEFAULT_STEP;
    if (stepMillisStr != null) {
      try {
        parsedStep = Duration.ofMillis(Long.parseLong(stepMillisStr));
      } catch (NumberFormatException e) {
        logger.log(
            Level.WARNING,
            "Invalid value for applicationinsights.internal.micrometer.step.millis: {0}, using default of {1}",
            new Object[] {stepMillisStr, DEFAULT_STEP});
      }
    }
    step = parsedStep;

    String namespaceValue = config.getString("applicationinsights.internal.micrometer.namespace");
    // Fallback to system property if not found in declarative config
    if (namespaceValue == null) {
      namespaceValue = System.getProperty("applicationinsights.internal.micrometer.namespace");
    }
    namespace = namespaceValue;
  }

  @Override
  public String prefix() {
    return "";
  }

  @Override
  public String get(String key) {
    return null;
  }

  @Override
  public Duration step() {
    return step;
  }

  @Nullable
  public String namespace() {
    return namespace;
  }
}
