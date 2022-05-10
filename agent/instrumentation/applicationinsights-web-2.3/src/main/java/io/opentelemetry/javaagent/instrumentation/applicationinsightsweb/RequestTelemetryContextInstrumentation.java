/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
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
import io.opentelemetry.instrumentation.api.field.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.function.BiConsumer;
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

  @SuppressWarnings("PrivateConstructorForUtilityClass")
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

  @SuppressWarnings("PrivateConstructorForUtilityClass")
  public static class GetTracestateAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This RequestTelemetryContext requestTelemetryContext,
        @Advice.Return(readOnly = false) Tracestate tracestate) {
      Span span =
          VirtualField.find(RequestTelemetryContext.class, Span.class).get(requestTelemetryContext);
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

  @SuppressWarnings("PrivateConstructorForUtilityClass")
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

  @SuppressWarnings("PrivateConstructorForUtilityClass")
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
