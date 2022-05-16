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

import com.microsoft.applicationinsights.extensibility.context.DeviceContext;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.extensibility.context.SessionContext;
import com.microsoft.applicationinsights.extensibility.context.UserContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class TelemetryContextInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.microsoft.applicationinsights.telemetry.TelemetryContext");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getUser"))
            .and(takesNoArguments()),
        TelemetryContextInstrumentation.class.getName() + "$GetUserAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getOperation"))
            .and(takesNoArguments()),
        TelemetryContextInstrumentation.class.getName() + "$GetOperationAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getSession"))
            .and(takesNoArguments()),
        TelemetryContextInstrumentation.class.getName() + "$GetSessionAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(named("getDevice"))
            .and(takesNoArguments()),
        TelemetryContextInstrumentation.class.getName() + "$GetDeviceAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(not(isStatic()))
            .and(not(named("getUser")))
            .and(not(named("getOperation")))
            .and(not(named("getSession")))
            .and(not(named("getDevice"))),
        TelemetryContextInstrumentation.class.getName() + "$OtherMethodsAdvice");
  }

  @SuppressWarnings("PrivateConstructorForUtilityClass")
  public static class GetUserAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This TelemetryContext telemetryContext, @Advice.Return UserContext userContext) {
      Span span = VirtualField.find(TelemetryContext.class, Span.class).get(telemetryContext);
      if (span != null) {
        VirtualField.find(UserContext.class, Span.class).set(userContext, span);
      }
    }
  }

  @SuppressWarnings("PrivateConstructorForUtilityClass")
  public static class GetOperationAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This TelemetryContext telemetryContext,
        @Advice.Return OperationContext operationContext) {
      Span span = VirtualField.find(TelemetryContext.class, Span.class).get(telemetryContext);
      if (span != null) {
        VirtualField.find(OperationContext.class, Span.class).set(operationContext, span);
      }
    }
  }

  @SuppressWarnings("PrivateConstructorForUtilityClass")
  public static class GetSessionAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This TelemetryContext telemetryContext,
        @Advice.Return SessionContext sessionContext) {
      Span span = VirtualField.find(TelemetryContext.class, Span.class).get(telemetryContext);
      if (span != null) {
        VirtualField.find(SessionContext.class, Span.class).set(sessionContext, span);
      }
    }
  }

  @SuppressWarnings("PrivateConstructorForUtilityClass")
  public static class GetDeviceAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This TelemetryContext telemetryContext,
        @Advice.Return DeviceContext deviceContext) {
      Span span = VirtualField.find(TelemetryContext.class, Span.class).get(telemetryContext);
      if (span != null) {
        VirtualField.find(DeviceContext.class, Span.class).set(deviceContext, span);
      }
    }
  }

  @SuppressWarnings("PrivateConstructorForUtilityClass")
  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This TelemetryContext telemetryContext, @Advice.Origin("#m") String methodName) {
      Span span = VirtualField.find(TelemetryContext.class, Span.class).get(telemetryContext);
      if (span != null) {
        LogOnce.logOnce(
            "ThreadContext.getRequestTelemetryContext().getRequestTelemetry().getContext()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.x agent");
      }
    }
  }
}
