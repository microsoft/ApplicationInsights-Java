// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.HelperResourceBuilder;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class ActuatorInstrumentationModule extends InstrumentationModule {

  // this instrumentation name is important since it is used to disable actuator-metrics
  // instrumentation
  public ActuatorInstrumentationModule() {
    super("ai-actuator-metrics");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("io.micrometer.core.instrument.Metrics");
  }

  @Override
  public void registerHelperResources(HelperResourceBuilder helperResourceBuilder) {
    // autoconfigure classes are loaded as resources using ClassPathResource
    // this line will make AzureMonitorAutoConfiguration available to all classloaders,
    // so that the bean class loader (different than the instrumented class loader) can load it
    helperResourceBuilder.registerForAllClassLoaders(
        "io/opentelemetry/javaagent/instrumentation/micrometer/ai/AzureMonitorAutoConfiguration.class");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new ActuatorInstrumentation());
  }
}
