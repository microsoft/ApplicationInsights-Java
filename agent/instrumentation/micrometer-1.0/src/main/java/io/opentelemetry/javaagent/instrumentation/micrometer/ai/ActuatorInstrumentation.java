// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.ArrayList;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public final class ActuatorInstrumentation implements TypeInstrumentation {

  private static final String SPRING_BOOT_3_METRICS_AUTO_CONFIGURATION =
      "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration";
  private static final String SPRING_BOOT_4_METRICS_AUTO_CONFIGURATION =
      "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration";

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.springframework.boot.autoconfigure.AutoConfigurationImportSelector");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("getCandidateConfigurations").and(returns(List.class)),
        ActuatorInstrumentation.class.getName() + "$GetCandidateConfigurationsAdvice");
  }

  @SuppressWarnings({
    "unused",
    "PrivateConstructorForUtilityClass"
  }) // value not used but required by API signature or framework
  public static class GetCandidateConfigurationsAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Return(readOnly = false) List<String> configurations) {
      if ((configurations.contains(SPRING_BOOT_3_METRICS_AUTO_CONFIGURATION)
              || configurations.contains(SPRING_BOOT_4_METRICS_AUTO_CONFIGURATION))
          && !configurations.contains(AzureMonitorAutoConfiguration.class.getName())) {
        List<String> configs = new ArrayList<>(configurations.size() + 1);
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
