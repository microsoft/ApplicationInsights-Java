// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class BaseTelemetryInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.microsoft.applicationinsights.telemetry.BaseTelemetry");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getProperties"))
            .and(takesNoArguments()),
        BaseTelemetryInstrumentation.class.getName() + "$GetPropertiesAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getContext"))
            .and(takesNoArguments()),
        BaseTelemetryInstrumentation.class.getName() + "$GetContextAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(not(named("getProperties")))
            .and(not(named("getContext"))),
        BaseTelemetryInstrumentation.class.getName() + "$OtherMethodsAdvice");
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class GetPropertiesAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This BaseTelemetry<?> telemetry,
        @Advice.Return(readOnly = false) Map<String, String> properties) {
      if (!(telemetry instanceof RequestTelemetry)) {
        return;
      }
      Span span =
          VirtualField.find(RequestTelemetry.class, Span.class).get((RequestTelemetry) telemetry);
      if (span != null) {
        properties = new SpanAttributeProperties(span);
      }
    }
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class GetContextAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This BaseTelemetry<?> telemetry, @Advice.Return TelemetryContext telemetryContext) {
      if (!(telemetry instanceof RequestTelemetry)) {
        return;
      }
      Span span =
          VirtualField.find(RequestTelemetry.class, Span.class).get((RequestTelemetry) telemetry);
      if (span != null) {
        VirtualField.find(TelemetryContext.class, Span.class).set(telemetryContext, span);
      }
    }
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This BaseTelemetry<?> telemetry, @Advice.Origin("#m") String methodName) {
      if (!(telemetry instanceof RequestTelemetry)) {
        return;
      }
      Span span =
          VirtualField.find(RequestTelemetry.class, Span.class).get((RequestTelemetry) telemetry);
      if (span != null) {
        LogOnce.logOnce(
            "ThreadContext.getRequestTelemetryContext().getRequestTelemetry()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.x agent");
      }
    }
  }
}
