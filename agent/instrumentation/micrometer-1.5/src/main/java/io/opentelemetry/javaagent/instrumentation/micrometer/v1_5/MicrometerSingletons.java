// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.v1_5;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.NamingConvention;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public final class MicrometerSingletons {

  private static final Logger logger = Logger.getLogger(MicrometerSingletons.class.getName());

  @Nullable private static final MeterRegistry METER_REGISTRY = getMeterRegistry(getConstructor());

  @Nullable
  public static MeterRegistry meterRegistry() {
    return METER_REGISTRY;
  }

  @Nullable
  private static MeterRegistry getMeterRegistry(
      Constructor<OpenTelemetryMeterRegistry> constructor) {
    if (constructor == null) {
      return null;
    }
    constructor.setAccessible(true);
    try {
      return constructor.newInstance(
          Clock.SYSTEM,
          TimeUnit.MILLISECONDS,
          new AzureMonitorNamingConvention(),
          GlobalOpenTelemetry.getMeter("io.opentelemetry.micrometer-1.5"));
    } catch (ReflectiveOperationException e) {
      logger.log(Level.FINE, e.getMessage(), e);
      return null;
    }
  }

  @Nullable
  private static Constructor<OpenTelemetryMeterRegistry> getConstructor() {
    try {
      return OpenTelemetryMeterRegistry.class.getDeclaredConstructor(
          Clock.class, TimeUnit.class, NamingConvention.class, Meter.class);
    } catch (NoSuchMethodException e) {
      logger.log(Level.FINE, e.getMessage(), e);
      return null;
    }
  }

  private MicrometerSingletons() {}
}
