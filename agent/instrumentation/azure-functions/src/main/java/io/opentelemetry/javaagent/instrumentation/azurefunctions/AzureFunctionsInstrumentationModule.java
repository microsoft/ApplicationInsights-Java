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

package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import static io.opentelemetry.javaagent.instrumentation.azurefunctions.InvocationRequestExtractAdapter.GETTER;
import static java.util.Collections.singletonList;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.bootstrap.AiLazyConfiguration;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class AzureFunctionsInstrumentationModule extends InstrumentationModule {

  public AzureFunctionsInstrumentationModule() {
    super("ai-azure-functions");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new AzureFunctionsInstrumentation());
  }

  @SuppressWarnings("unused")
  private static class AzureFunctionsInstrumentation implements TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return named("com.microsoft.azure.functions.worker.handler.InvocationRequestHandler");
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
          isMethod()
              .and(named("execute"))
              .and(
                  takesArgument(
                      0, named("com.microsoft.azure.functions.rpc.messages.InvocationRequest"))),
          AzureFunctionsInstrumentation.class.getName() + "$InvocationRequestAdvice");
    }

    @SuppressWarnings("MustBeClosedChecker")
    public static class InvocationRequestAdvice {
      @Advice.OnMethodEnter(suppress = Throwable.class)
      public static Scope methodEnter(@Advice.Argument(0) Object request)
          throws ReflectiveOperationException {
        AiLazyConfiguration.lazyLoad();

        Object traceContext = InvocationRequestExtractAdapter.getTraceContextMethod.invoke(request);
        Context extractedContext =
            GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.root(), traceContext, GETTER);
        SpanContext spanContext = Span.fromContext(extractedContext).getSpanContext();

        return Context.current().with(Span.wrap(spanContext)).makeCurrent();
      }

      @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
      public static void methodExit(@Advice.Enter Scope scope) {
        scope.close();
      }
    }
  }
}
