// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.microsoft.applicationinsights.agent.bootstrap.AzureFunctions;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

class FunctionEnvironmentReloadInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named(
        "com.microsoft.azure.functions.worker.handler.FunctionEnvironmentReloadRequestHandler");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("execute")),
        FunctionEnvironmentReloadInstrumentation.class.getName() + "$ExecuteAdvice");
  }

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass"})
  public static class ExecuteAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void methodExit() {
      AzureFunctions.configureOnce();
    }
  }
}
