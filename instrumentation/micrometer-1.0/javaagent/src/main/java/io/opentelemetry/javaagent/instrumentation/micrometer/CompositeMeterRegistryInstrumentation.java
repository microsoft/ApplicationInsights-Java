/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.micrometer;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public final class CompositeMeterRegistryInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("io.micrometer.core.instrument.composite.CompositeMeterRegistry");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isMethod()
            .and(named("add"))
            .and(takesArguments(1))
            .and(takesArgument(0, named("io.micrometer.core.instrument.MeterRegistry"))),
        CompositeMeterRegistryInstrumentation.class.getName() + "$AddAdvice");
  }

  public static class AddAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class, skipOn = Advice.OnNonDefaultValue.class)
    public static boolean onEnter(@Advice.Argument(0) final MeterRegistry registry) {
      return registry
          .getClass()
          .getName()
          .equals("io.micrometer.azuremonitor.AzureMonitorMeterRegistry");
    }

    // this is to make muzzle think that AzureMonitorMeterRegistry is needed so that this
    // instrumentation is not applied when MetricsInstrumentation would not also be applied
    public static Object muzzleCheck() {
      return AzureMonitorMeterRegistry.INSTANCE;
    }
  }
}
