/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.auto.micrometer;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public final class MetricsInstrumentation extends Instrumenter.Default {

  public MetricsInstrumentation() {
    super("micrometer");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("io.micrometer.core.instrument.Metrics");
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      packageName + ".AzureMonitorMeterRegistry",
      packageName + ".AzureMonitorNamingConvention",
      packageName + ".AzureMonitorRegistryConfig",
      packageName + ".DaemonThreadFactory"
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isTypeInitializer(), MetricsInstrumentation.class.getName() + "$StaticInitAdvice");
  }

  public static class StaticInitAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(
        @Advice.FieldValue("globalRegistry") final CompositeMeterRegistry globalRegistry) {
      globalRegistry.add(AzureMonitorMeterRegistry.INSTANCE);
    }
  }
}
