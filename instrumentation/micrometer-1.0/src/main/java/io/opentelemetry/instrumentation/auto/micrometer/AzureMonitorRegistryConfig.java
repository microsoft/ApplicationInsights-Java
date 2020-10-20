/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.auto.micrometer;

import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.opentelemetry.instrumentation.api.config.Config;
import java.time.Duration;

public class AzureMonitorRegistryConfig implements StepRegistryConfig {

  private final Duration step;

  public AzureMonitorRegistryConfig() {
    // TODO add Config.get().getIntegerProperty()
    String stepStr = Config.get().getProperty("micrometer.step.millis");
    if (stepStr != null && !stepStr.isEmpty()) {
      step = Duration.ofMillis(Integer.parseInt(stepStr));
    } else {
      step = Duration.ofMillis(60000);
    }
  }

  @Override
  public String prefix() {
    return "";
  }

  @Override
  public String get(final String key) {
    return null;
  }

  @Override
  public Duration step() {
    return step;
  }
}
