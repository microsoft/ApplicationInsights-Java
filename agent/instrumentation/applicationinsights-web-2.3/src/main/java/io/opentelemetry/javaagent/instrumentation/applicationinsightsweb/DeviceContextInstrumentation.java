// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
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

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
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

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
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
