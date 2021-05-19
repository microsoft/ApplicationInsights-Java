/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.azuresdk;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.ClassLoaderMatcher.hasClassesNamed;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.List;
import java.util.Map;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class AzureSdkInstrumentationModule extends InstrumentationModule {
  public AzureSdkInstrumentationModule() {
    super("azure-core", "azure-core-1.14");
  }

  @Override
  public boolean isHelperClass(String className) {
    return className.startsWith("com.azure.core.tracing.opentelemetry.");
  }

  @Override
  public String[] helperResourceNames() {
    return new String[] {
      "META-INF/services/com.azure.core.http.policy.AfterRetryPolicyProvider",
      "META-INF/services/com.azure.core.util.tracing.Tracer"
    };
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("com.azure.core.util.tracing.Tracer")
        .and(not(hasClassesNamed("com.azure.core.tracing.opentelemetry.OpenTelemetryTracer")));
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(new EmptyTypeInstrumentation(), new AzureHttpClientInstrumentation());
  }

  public static class EmptyTypeInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      // we cannot use com.azure.core.http.policy.AfterRetryPolicyProvider
      // or com.azure.core.util.tracing.Tracer here because we inject classes that implement these
      // interfaces, causing the first one of these interfaces to be transformed to cause itself to
      // be loaded (again), which leads to duplicate class definition error after the interface is
      // transformed and the triggering class loader tries to load it.
      return named("com.azure.core.http.policy.HttpPolicyProviders")
          .or(named("com.azure.core.util.tracing.TracerProxy"));
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      // Nothing to instrument, no methods to match
      return emptyMap();
    }
  }
}
