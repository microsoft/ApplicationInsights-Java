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

import com.microsoft.applicationinsights.extensibility.context.UserContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class TelemetryContextInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("com.microsoft.applicationinsights.telemetry.TelemetryContext");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getUser"))
            .and(takesNoArguments()),
        TelemetryContextInstrumentation.class.getName() + "$GetUserAdvice");
    transformers.put(
        isMethod().and(isPublic()).and(not(isStatic())).and(not(named("getUser"))),
        TelemetryContextInstrumentation.class.getName() + "$OtherMethodsAdvice");
    return transformers;
  }

  public static class GetUserAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This TelemetryContext telemetryContext, @Advice.Return UserContext userContext) {
      Span span =
          InstrumentationContext.get(TelemetryContext.class, Span.class).get(telemetryContext);
      if (span != null) {
        InstrumentationContext.get(UserContext.class, Span.class).put(userContext, span);
      }
    }
  }

  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This TelemetryContext telemetryContext, @Advice.Origin("#m") String methodName) {
      Span span =
          InstrumentationContext.get(TelemetryContext.class, Span.class).get(telemetryContext);
      if (span != null) {
        LogOnce.logOnce(
            "ThreadContext.getRequestTelemetryContext().getRequestTelemetry().getContext()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.0 agent");
      }
    }
  }
}
