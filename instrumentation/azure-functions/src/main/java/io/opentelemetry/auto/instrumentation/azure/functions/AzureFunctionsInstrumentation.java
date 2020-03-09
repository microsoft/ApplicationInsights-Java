/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.auto.instrumentation.azure.functions;

import static io.opentelemetry.auto.instrumentation.azure.functions.InvocationRequestExtractAdapter.GETTER;
import static io.opentelemetry.auto.instrumentation.azure.functions.InvocationRequestExtractAdapter.TRACER;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.google.auto.service.AutoService;
import io.grpc.Context;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.auto.tooling.Instrumenter;
import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.DefaultSpan;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.TracingContextUtils;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class AzureFunctionsInstrumentation extends Instrumenter.Default {

  public AzureFunctionsInstrumentation() {
    super("azure-functions");
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return named("com.microsoft.azure.functions.worker.handler.InvocationRequestHandler");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isMethod()
            .and(named("execute"))
            .and(
                takesArgument(
                    0, named("com.microsoft.azure.functions.rpc.messages.InvocationRequest"))),
        AzureFunctionsInstrumentation.class.getName() + "$InvocationRequestAdvice");
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {packageName + ".InvocationRequestExtractAdapter"};
  }

  public static class InvocationRequestAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Scope methodEnter(@Advice.Argument(0) final Object request)
        throws ReflectiveOperationException {

      final Object traceContext =
          InvocationRequestExtractAdapter.getTraceContextMethod.invoke(request);

      final Context extractedContext =
          OpenTelemetry.getPropagators()
              .getHttpTextFormat()
              .extract(Context.ROOT, traceContext, GETTER);
      final SpanContext spanContext = TracingContextUtils.getSpan(extractedContext).getContext();

      return TRACER.withSpan(DefaultSpan.create(spanContext));
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(@Advice.Enter final Scope scope) {
      scope.close();
    }
  }
}
