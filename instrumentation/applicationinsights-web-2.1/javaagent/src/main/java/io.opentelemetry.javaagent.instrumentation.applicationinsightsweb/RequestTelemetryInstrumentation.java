/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class RequestTelemetryInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("com.microsoft.applicationinsights.telemetry.RequestTelemetry");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("setName"))
            .and(takesArguments(1)),
        RequestTelemetryInstrumentation.class.getName() + "$SetNameAdvice");
    transformers.put(
        isMethod().and(isPublic()).and(not(isStatic())).and(not(named("setName"))),
        RequestTelemetryInstrumentation.class.getName() + "$OtherMethodsAdvice");
    return transformers;
  }

  public static class SetNameAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This RequestTelemetry requestTelemetry, @Advice.Argument(0) String name) {
      Span span =
          InstrumentationContext.get(RequestTelemetry.class, Span.class).get(requestTelemetry);
      if (span != null) {
        span.updateName(name);
      }
    }
  }

  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This RequestTelemetry requestTelemetry, @Advice.Origin("#m") String methodName) {
      Span span =
          InstrumentationContext.get(RequestTelemetry.class, Span.class).get(requestTelemetry);
      if (span != null) {
        throw new RuntimeException(
            "ThreadContext.getRequestTelemetryContext().getRequestTelemetry()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.0 agent");
      }
    }
  }
}
