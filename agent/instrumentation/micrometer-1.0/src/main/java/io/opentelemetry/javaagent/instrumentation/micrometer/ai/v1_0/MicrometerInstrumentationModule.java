// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.micrometer.ai.v1_0;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.Arrays;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class MicrometerInstrumentationModule extends InstrumentationModule {

  // this instrumentation name is important since it is used to disable micrometer instrumentation
  public MicrometerInstrumentationModule() {
    super("ai-micrometer");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("io.micrometer.core.instrument.Metrics")
        // added in 1.5
        .and(not(hasClassesNamed("io.micrometer.core.instrument.config.validate.Validated")));
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(new MetricsInstrumentation(), new CompositeMeterRegistryInstrumentation());
  }
}
