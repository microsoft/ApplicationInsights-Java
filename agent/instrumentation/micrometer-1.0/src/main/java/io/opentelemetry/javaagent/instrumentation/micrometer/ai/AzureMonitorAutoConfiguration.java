// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import io.micrometer.core.instrument.Clock;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(CompositeMeterRegistryAutoConfiguration.class)
// configure after SimpleMeterRegistry is registered, otherwise SimpleMeterRegistry will be
// suppressed by the existence of the MeterRegistry created here, which can alter the spring boot
// actuator scraping endpoint behavior (since AzureMonitorMeterRegistry is a delta
// StepMeterRegistry, while SimpleMeterRegistry is cumulative)
@AutoConfigureAfter({MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class})
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(AzureMonitorMeterRegistry.class)
public class AzureMonitorAutoConfiguration {

  @Bean
  public AzureMonitorMeterRegistry azureMonitorMeterRegistry() {
    return AzureMonitorMeterRegistry.INSTANCE;
  }
}
