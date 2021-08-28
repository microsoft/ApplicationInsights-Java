/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.micrometer;

import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.opentelemetry.instrumentation.api.config.Config;
import java.time.Duration;

public class AzureMonitorRegistryConfig implements StepRegistryConfig {

  private final Duration step;

  public AzureMonitorRegistryConfig() {
    step = Config.get().getDuration("otel.micrometer.step.millis", Duration.ofSeconds(60));
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
