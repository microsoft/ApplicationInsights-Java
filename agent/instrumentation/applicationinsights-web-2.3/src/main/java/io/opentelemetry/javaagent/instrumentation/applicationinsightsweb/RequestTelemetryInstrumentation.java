// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class RequestTelemetryInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.microsoft.applicationinsights.telemetry.RequestTelemetry");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("setName"))
            .and(takesArguments(1)),
        RequestTelemetryInstrumentation.class.getName() + "$SetNameAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("setSuccess"))
            .and(takesArguments(1)),
        RequestTelemetryInstrumentation.class.getName() + "$SetSuccessAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("setSource"))
            .and(takesArguments(1)),
        RequestTelemetryInstrumentation.class.getName() + "$SetSourceAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(isPublic()).and(not(isStatic())).and(named("getId")).and(takesNoArguments()),
        RequestTelemetryInstrumentation.class.getName() + "$GetIdAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(not(named("setName")))
            .and(not(named("setSuccess")))
            .and(not(named("setSource")))
            .and(not(named("getId"))),
        RequestTelemetryInstrumentation.class.getName() + "$OtherMethodsAdvice");
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class SetNameAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This RequestTelemetry requestTelemetry, @Advice.Argument(0) String name) {
      Span span = VirtualField.find(RequestTelemetry.class, Span.class).get(requestTelemetry);
      if (span != null) {
        span.updateName(name);
      }
    }
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class SetSuccessAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This RequestTelemetry requestTelemetry, @Advice.Argument(0) boolean success) {
      Span span = VirtualField.find(RequestTelemetry.class, Span.class).get(requestTelemetry);
      if (span != null) {
        span.setStatus(success ? StatusCode.OK : StatusCode.ERROR);
      }
    }
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class SetSourceAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This RequestTelemetry requestTelemetry, @Advice.Argument(0) String source) {
      Span span = VirtualField.find(RequestTelemetry.class, Span.class).get(requestTelemetry);
      if (span != null) {
        span.setAttribute("applicationinsights.internal.source", source);
      }
    }
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class GetIdAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This RequestTelemetry requestTelemetry,
        @Advice.Return(readOnly = false) String id) {
      Span span = VirtualField.find(RequestTelemetry.class, Span.class).get(requestTelemetry);
      if (span != null) {
        id = span.getSpanContext().getSpanId();
      }
    }
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This RequestTelemetry requestTelemetry, @Advice.Origin("#m") String methodName) {
      Span span = VirtualField.find(RequestTelemetry.class, Span.class).get(requestTelemetry);
      if (span != null) {
        LogOnce.logOnce(
            "ThreadContext.getRequestTelemetryContext().getRequestTelemetry()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.x agent");
      }
    }
  }
}
