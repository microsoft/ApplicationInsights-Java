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

import com.microsoft.applicationinsights.extensibility.context.DeviceContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

public class DeviceContextInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return ElementMatchers.named(
        "com.microsoft.applicationinsights.extensibility.context.DeviceContext");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        ElementMatchers.isMethod()
            .and(ElementMatchers.isPublic())
            .and(ElementMatchers.not(ElementMatchers.isStatic()))
            .and(ElementMatchers.named("setOperatingSystem"))
            .and(ElementMatchers.takesArguments(1)),
        DeviceContextInstrumentation.class.getName() + "$SetOperatingSystemAdvice");
    transformer.applyAdviceToMethod(
        ElementMatchers.isMethod()
            .and(ElementMatchers.isPublic())
            .and(ElementMatchers.not(ElementMatchers.isStatic()))
            .and(ElementMatchers.named("setOperatingSystemVersion"))
            .and(ElementMatchers.takesArguments(1)),
        DeviceContextInstrumentation.class.getName() + "$SetOperatingSystemVersionAdvice");
    transformer.applyAdviceToMethod(
        ElementMatchers.isMethod()
            .and(ElementMatchers.isPublic())
            .and(ElementMatchers.not(ElementMatchers.isStatic()))
            .and(ElementMatchers.not(ElementMatchers.named("setOperatingSystem")))
            .and(ElementMatchers.not(ElementMatchers.named("setOperatingSystemVersion"))),
        DeviceContextInstrumentation.class.getName() + "$OtherMethodsAdvice");
  }

  @SuppressWarnings("PrivateConstructorForUtilityClass")
  public static class SetOperatingSystemAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This DeviceContext deviceContext, @Advice.Argument(0) String operatingSystem) {
      Span span = VirtualField.find(DeviceContext.class, Span.class).get(deviceContext);
      if (span != null) {
        span.setAttribute("applicationinsights.internal.operating_system", operatingSystem);
      }
    }
  }

  @SuppressWarnings("PrivateConstructorForUtilityClass")
  public static class SetOperatingSystemVersionAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This DeviceContext deviceContext,
        @Advice.Argument(0) String operatingSystemVersion) {
      Span span = VirtualField.find(DeviceContext.class, Span.class).get(deviceContext);
      if (span != null) {
        span.setAttribute(
            "applicationinsights.internal.operating_system_version", operatingSystemVersion);
      }
    }
  }

  @SuppressWarnings("PrivateConstructorForUtilityClass")
  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This DeviceContext deviceContext, @Advice.Origin("#m") String methodName) {
      Span span = VirtualField.find(DeviceContext.class, Span.class).get(deviceContext);
      if (span != null) {
        LogOnce.logOnce(
            "ThreadContext.getRequestTelemetryContext().getRequestTelemetry().getContext().getDevice()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.x agent");
      }
    }
  }
}
