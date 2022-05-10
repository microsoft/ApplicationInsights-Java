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

import com.microsoft.applicationinsights.telemetry.BaseTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.field.VirtualField;
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

  @SuppressWarnings("PrivateConstructorForUtilityClass")
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

  @SuppressWarnings("PrivateConstructorForUtilityClass")
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

  @SuppressWarnings("PrivateConstructorForUtilityClass")
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
