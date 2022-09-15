// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.microsoft.applicationinsights.extensibility.context.SessionContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class SessionContextInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.microsoft.applicationinsights.extensibility.context.SessionContext");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(isPublic()).and(not(isStatic())).and(named("setId")).and(takesArguments(1)),
        SessionContextInstrumentation.class.getName() + "$SetIdAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(isPublic()).and(not(isStatic())).and(not(named("setId"))),
        SessionContextInstrumentation.class.getName() + "$OtherMethodsAdvice");
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class SetIdAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This SessionContext sessionContext, @Advice.Argument(0) String id) {
      Span span = VirtualField.find(SessionContext.class, Span.class).get(sessionContext);
      if (span != null) {
        span.setAttribute("applicationinsights.internal.session_id", id);
      }
    }
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This SessionContext sessionContext, @Advice.Origin("#m") String methodName) {
      Span span = VirtualField.find(SessionContext.class, Span.class).get(sessionContext);
      if (span != null) {
        LogOnce.logOnce(
            "ThreadContext.getRequestTelemetryContext().getRequestTelemetry().getContext().getSession()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.x agent");
      }
    }
  }
}
