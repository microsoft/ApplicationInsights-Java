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
import com.microsoft.applicationinsights.web.internal.correlation.tracecontext.Tracestate;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;
import io.opentelemetry.javaagent.instrumentation.api.InstrumentationContext;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
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
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getTracestate"))
            .and(takesNoArguments()),
        RequestTelemetryContextInstrumentation.class.getName() + "$GetTracestateAdvice");
    transformers.put(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getTraceflag"))
            .and(takesNoArguments()),
        RequestTelemetryContextInstrumentation.class.getName() + "$GetTraceflagAdvice");
    transformers.put(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(not(named("getHttpRequestTelemetry")))
            .and(not(named("getTracestate")))
            .and(not(named("getTraceflag"))),
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

  public static class GetTracestateAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This RequestTelemetryContext requestTelemetryContext,
        @Advice.Return(readOnly = false) Tracestate tracestate) {
      Span span =
          InstrumentationContext.get(RequestTelemetryContext.class, Span.class)
              .get(requestTelemetryContext);
      if (span != null) {
        TraceState traceState = span.getSpanContext().getTraceState();
        Tracestate parent;
        if (traceState.isEmpty()) {
          parent = null;
        } else {
          TracestateBuilder builder = new TracestateBuilder();
          traceState.forEach(builder);
          parent = new Tracestate(builder.toString());
        }
        // this is what 2.x SDK does
        String appId = AiAppId.getAppId();
        if (appId != null && !appId.isEmpty()) {
          tracestate = new Tracestate(parent, "az", appId);
        } else {
          tracestate = parent;
        }
      }
    }
  }

  public static class TracestateBuilder implements BiConsumer<String, String> {

    private final StringBuilder stringBuilder = new StringBuilder();

    @Override
    public void accept(String key, String value) {
      if (stringBuilder.length() != 0) {
        stringBuilder.append(',');
      }
      stringBuilder.append(key).append('=').append(value);
    }

    @Override
    public String toString() {
      return stringBuilder.toString();
    }
  }

  public static class GetTraceflagAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This RequestTelemetryContext requestTelemetryContext,
        @Advice.Return(readOnly = false) int traceflag) {
      Span span =
          InstrumentationContext.get(RequestTelemetryContext.class, Span.class)
              .get(requestTelemetryContext);
      if (span != null) {
        traceflag = span.getSpanContext().getTraceFlags().asByte();
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
        LogOnce.logOnce(
            "ThreadContext.getRequestTelemetryContext()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.0 agent");
      }
    }
  }
}
