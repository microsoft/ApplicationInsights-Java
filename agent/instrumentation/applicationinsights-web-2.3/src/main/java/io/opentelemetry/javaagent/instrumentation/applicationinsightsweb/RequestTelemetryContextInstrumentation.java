// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext;
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Tracestate;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class RequestTelemetryContextInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.microsoft.applicationinsights.web.internal.RequestTelemetryContext");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getHttpRequestTelemetry"))
            .and(takesNoArguments()),
        RequestTelemetryContextInstrumentation.class.getName() + "$GetRequestTelemetryAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getTracestate"))
            .and(takesNoArguments()),
        RequestTelemetryContextInstrumentation.class.getName() + "$GetTracestateAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getTraceflag"))
            .and(takesNoArguments()),
        RequestTelemetryContextInstrumentation.class.getName() + "$GetTraceflagAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(not(named("getHttpRequestTelemetry")))
            .and(not(named("getTracestate")))
            .and(not(named("getTraceflag"))),
        RequestTelemetryContextInstrumentation.class.getName() + "$OtherMethodsAdvice");
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class GetRequestTelemetryAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This RequestTelemetryContext requestTelemetryContext,
        @Advice.Return RequestTelemetry requestTelemetry) {
      Span span =
          VirtualField.find(RequestTelemetryContext.class, Span.class).get(requestTelemetryContext);
      if (span != null) {
        VirtualField.find(RequestTelemetry.class, Span.class).set(requestTelemetry, span);
      }
    }
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class GetTracestateAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This RequestTelemetryContext requestTelemetryContext,
        @Advice.Return(readOnly = false) Tracestate tracestate) {
      Span span =
          VirtualField.find(RequestTelemetryContext.class, Span.class).get(requestTelemetryContext);
      if (span != null) {
        TraceState traceState = span.getSpanContext().getTraceState();
        if (traceState.isEmpty()) {
          tracestate = null;
        } else {
          TracestateBuilder builder = new TracestateBuilder();
          traceState.forEach(builder);
          tracestate = new Tracestate(builder.toString());
        }
      }
    }
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class GetTraceflagAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This RequestTelemetryContext requestTelemetryContext,
        @Advice.Return(readOnly = false) int traceflag) {
      Span span =
          VirtualField.find(RequestTelemetryContext.class, Span.class).get(requestTelemetryContext);
      if (span != null) {
        traceflag = span.getSpanContext().getTraceFlags().asByte();
      }
    }
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This RequestTelemetryContext requestTelemetryContext,
        @Advice.Origin("#m") String methodName) {
      Span span =
          VirtualField.find(RequestTelemetryContext.class, Span.class).get(requestTelemetryContext);
      if (span != null) {
        LogOnce.logOnce(
            "ThreadContext.getRequestTelemetryContext()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.x agent");
      }
    }
  }
}
