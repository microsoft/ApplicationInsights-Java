// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.instrumentation.api.incubator.config.internal.DeclarativeConfigUtil;
import java.time.Duration;
import javax.annotation.Nullable;

public class AzureMonitorRegistryConfig implements StepRegistryConfig {

  private final Duration step;
  @Nullable private final String namespace;

  public static final AzureMonitorRegistryConfig INSTANCE = new AzureMonitorRegistryConfig();

  private AzureMonitorRegistryConfig() {
    DeclarativeConfigProperties config =
        DeclarativeConfigUtil.getInstrumentationConfig(GlobalOpenTelemetry.get(), "micrometer");
    
    // Get step duration in milliseconds, default to 60 seconds
    String stepMillisStr = config.getString("applicationinsights.internal.micrometer.step.millis");
    Duration stepValue;
    if (stepMillisStr != null) {
      try {
        stepValue = Duration.ofMillis(Long.parseLong(stepMillisStr));
      } catch (NumberFormatException e) {
        stepValue = Duration.ofSeconds(60);
      }
    } else {
      stepValue = Duration.ofSeconds(60);
    }
    step = stepValue;
    
    namespace = config.getString("applicationinsights.internal.micrometer.namespace");
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
