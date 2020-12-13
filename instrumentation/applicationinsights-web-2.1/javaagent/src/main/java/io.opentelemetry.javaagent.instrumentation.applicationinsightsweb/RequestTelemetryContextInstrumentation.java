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
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class RequestTelemetryContextInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("com.microsoft.applicationinsights.web.internal.RequestTelemetryContext");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getHttpRequestTelemetry"))
            .and(takesNoArguments()),
        RequestTelemetryContextInstrumentation.class.getName() + "$GetRequestTelemetryAdvice");
    transformers.put(
        isMethod().and(isPublic()).and(not(isStatic())).and(not(named("getHttpRequestTelemetry"))),
        RequestTelemetryContextInstrumentation.class.getName() + "$OtherMethodsAdvice");
    return transformers;
  }

  public static class GetRequestTelemetryAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This RequestTelemetryContext requestTelemetryContext,
        @Advice.Return RequestTelemetry requestTelemetry) {
      Span span =
          InstrumentationContext.get(RequestTelemetryContext.class, Span.class)
              .get(requestTelemetryContext);
      if (span != null) {
        InstrumentationContext.get(RequestTelemetry.class, Span.class).put(requestTelemetry, span);
      }
    }
  }

  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This RequestTelemetryContext requestTelemetryContext,
        @Advice.Origin("#m") String methodName) {
      Span span =
          InstrumentationContext.get(RequestTelemetryContext.class, Span.class)
              .get(requestTelemetryContext);
      if (span != null) {
        throw new RuntimeException(
            "ThreadContext.getRequestTelemetryContext()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.0 agent");
      }
    }
  }
}
