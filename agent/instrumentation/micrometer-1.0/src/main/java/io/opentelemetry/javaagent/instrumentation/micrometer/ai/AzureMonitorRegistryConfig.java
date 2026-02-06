// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import io.micrometer.core.instrument.step.StepRegistryConfig;
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
    // Read configuration from system property (set by AiConfigCustomizer)
    String stepConfigValue =
        System.getProperty("applicationinsights.internal.micrometer.step.millis");

    Duration configuredStep = DEFAULT_STEP;
    if (stepConfigValue != null) {
      try {
        configuredStep = Duration.ofMillis(Long.parseLong(stepConfigValue));
      } catch (NumberFormatException ex) {
        logger.log(
            Level.WARNING,
            "Invalid value for applicationinsights.internal.micrometer.step.millis: {0}, using default of {1}",
            new Object[] {stepConfigValue, DEFAULT_STEP});
      }
    }
    step = configuredStep;

    namespace = System.getProperty("applicationinsights.internal.micrometer.namespace");
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
