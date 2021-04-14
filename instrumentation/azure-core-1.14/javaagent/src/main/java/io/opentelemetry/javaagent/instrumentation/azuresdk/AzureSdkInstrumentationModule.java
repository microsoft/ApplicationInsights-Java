/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.azuresdk;

import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.ClassLoaderMatcher.hasClassesNamed;
import static java.util.Collections.emptyMap;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
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
    return Collections.singletonList(new EmptyTypeInstrumentation());
  }

  public static class EmptyTypeInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<? super TypeDescription> typeMatcher() {
      // we cannot use com.azure.core.util.tracing.Tracer here because one of the classes that we
      // inject implements this interface, causing the interface to be loaded while it's being
      // transformed, which leads to duplicate class definition error after the interface is
      // transformed and the triggering class loader tries to load it.
      return named("com.azure.core.util.tracing.TracerProxy");
    }

    @Override
    public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
      // Nothing to instrument, no methods to match
      return emptyMap();
    }
  }
}
