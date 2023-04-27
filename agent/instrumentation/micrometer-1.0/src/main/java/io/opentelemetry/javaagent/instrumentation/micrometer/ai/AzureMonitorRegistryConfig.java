// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;
import java.time.Duration;

public class AzureMonitorRegistryConfig implements StepRegistryConfig {

  private final Duration step;
  private final String namespace;

  public AzureMonitorRegistryConfig() {
    step =
        InstrumentationConfig.get()
            .getDuration(
                "applicationinsights.internal.micrometer.step.millis", Duration.ofSeconds(60));
    namespace =
        InstrumentationConfig.get()
            .getString("applicationinsights.internal.micrometer.namespace", "");
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

  public String namespace() {
    return namespace;
  }
}
