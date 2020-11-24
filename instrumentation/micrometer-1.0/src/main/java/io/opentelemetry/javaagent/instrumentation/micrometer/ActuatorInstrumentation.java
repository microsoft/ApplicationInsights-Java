/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.micrometer;

import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public final class ActuatorInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.springframework.boot.autoconfigure.AutoConfigurationImportSelector");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        named("getCandidateConfigurations").and(returns(List.class)),
        ActuatorInstrumentation.class.getName() + "$GetCandidateConfigurationsAdvice");
  }

  public static class GetCandidateConfigurationsAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Return(readOnly = false) List<String> configurations) {
      if (configurations.contains(
          "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration")) {
        final List<String> configs = new ArrayList<>(configurations.size() + 1);
        configs.addAll(configurations);
        // using class reference here so that muzzle will consider it a dependency of this advice
        configs.add(AzureMonitorAutoConfiguration.class.getName());
        configs.remove(
            "com.microsoft.azure.spring.autoconfigure.metrics.AzureMonitorMetricsExportAutoConfiguration");
        configurations = configs;
      }
    }
  }
}
