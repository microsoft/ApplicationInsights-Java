// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import io.micrometer.core.instrument.step.StepRegistryConfig;
import java.time.Duration;
import javax.annotation.Nullable;

public class AzureMonitorRegistryConfig implements StepRegistryConfig {

  private final Duration step;
  @Nullable private final String namespace;

  public static final AzureMonitorRegistryConfig INSTANCE = new AzureMonitorRegistryConfig();

  private AzureMonitorRegistryConfig() {
    Duration defaultStep = Duration.ofSeconds(60);

    String stepMillisString =
        System.getProperty("applicationinsights.internal.micrometer.step.millis");
    Duration resolvedStep = defaultStep;
    if (stepMillisString != null) {
      try {
        long millis = Long.parseLong(stepMillisString);
        resolvedStep = Duration.ofMillis(millis);
      } catch (NumberFormatException ignored) {
        // use default step
      }
    }
    step = resolvedStep;

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
