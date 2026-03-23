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
    DeclarativeConfigProperties micrometerConfig =
        DeclarativeConfigUtil.getInstrumentationConfig(
                GlobalOpenTelemetry.get(), "applicationinsights")
            .get("internal")
            .get("micrometer");
    step = Duration.ofMillis(micrometerConfig.get("step").getLong("millis", 60000));
    namespace = micrometerConfig.getString("namespace");
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
