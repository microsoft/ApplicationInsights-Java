/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.micrometer;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.ClassLoaderMatcher;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Arrays;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class ActuatorInstrumentationModule extends InstrumentationModule {

  public ActuatorInstrumentationModule() {
    super("actuator-metrics");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return ClassLoaderMatcher.hasClassesNamed("io.micrometer.core.instrument.Metrics");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(new ActuatorInstrumentation(), new ClassPathResourceInstrumentation());
  }
}
