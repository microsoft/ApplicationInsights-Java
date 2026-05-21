// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import io.micrometer.core.instrument.Clock;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(
    name = {
      "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
      "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration"
    })
// configure after SimpleMeterRegistry is registered, otherwise SimpleMeterRegistry will be
// suppressed by the existence of the MeterRegistry created here, which can alter the spring boot
// actuator scraping endpoint behavior (since AzureMonitorMeterRegistry is a delta
// StepMeterRegistry, while SimpleMeterRegistry is cumulative)
@AutoConfigureAfter(
    name = {
      "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
      "org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration",
      "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
      "org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration"
    })
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(AzureMonitorMeterRegistry.class)
public class AzureMonitorAutoConfiguration {

  @Bean
  public AzureMonitorMeterRegistry azureMonitorMeterRegistry() {
    return AzureMonitorMeterRegistry.INSTANCE;
  }
}
