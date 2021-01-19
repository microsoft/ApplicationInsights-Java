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

import com.microsoft.applicationinsights.telemetry.BaseTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
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

public class BaseTelemetryInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("com.microsoft.applicationinsights.telemetry.BaseTelemetry");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getProperties"))
            .and(takesNoArguments()),
        BaseTelemetryInstrumentation.class.getName() + "$GetPropertiesAdvice");
    transformers.put(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getContext"))
            .and(takesNoArguments()),
        BaseTelemetryInstrumentation.class.getName() + "$GetContextAdvice");
    transformers.put(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(not(named("getProperties")))
            .and(not(named("getContext"))),
        BaseTelemetryInstrumentation.class.getName() + "$OtherMethodsAdvice");
    return transformers;
  }

  public static class GetPropertiesAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This BaseTelemetry<?> telemetry,
        @Advice.Return(readOnly = false) Map<String, String> properties) {
      if (!(telemetry instanceof RequestTelemetry)) {
        return;
      }
      Span span =
          InstrumentationContext.get(RequestTelemetry.class, Span.class)
              .get((RequestTelemetry) telemetry);
      if (span != null) {
        properties = new SpanAttributeProperties(span);
      }
    }
  }

  public static class GetContextAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This BaseTelemetry<?> telemetry, @Advice.Return TelemetryContext telemetryContext) {
      if (!(telemetry instanceof RequestTelemetry)) {
        return;
      }
      Span span =
          InstrumentationContext.get(RequestTelemetry.class, Span.class)
              .get((RequestTelemetry) telemetry);
      if (span != null) {
        InstrumentationContext.get(TelemetryContext.class, Span.class).put(telemetryContext, span);
      }
    }
  }

  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This BaseTelemetry<?> telemetry, @Advice.Origin("#m") String methodName) {
      if (!(telemetry instanceof RequestTelemetry)) {
        return;
      }
      Span span =
          InstrumentationContext.get(RequestTelemetry.class, Span.class)
              .get((RequestTelemetry) telemetry);
      if (span != null) {
        LogOnce.logOnce(
            "ThreadContext.getRequestTelemetryContext().getRequestTelemetry()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.0 agent");
      }
    }
  }
}
