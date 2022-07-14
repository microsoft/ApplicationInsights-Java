package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import static io.opentelemetry.javaagent.instrumentation.azurefunctions.InvocationRequestExtractAdapter.GETTER;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.microsoft.applicationinsights.agent.bootstrap.AzureFunctions;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import javax.annotation.Nullable;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@SuppressWarnings("unused")
class InvocationInstrumentation implements TypeInstrumentation {

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
        InvocationInstrumentation.class.getName() + "$ExecuteAdvice");
  }

  @SuppressWarnings({"PrivateConstructorForUtilityClass", "MustBeClosedChecker"})
  public static class ExecuteAdvice {
    @Nullable
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Scope methodEnter(@Advice.Argument(0) Object request)
        throws ReflectiveOperationException {

      if (!AzureFunctions.hasConnectionString()) {
        return null;
      }

      Object traceContext = InvocationRequestExtractAdapter.getTraceContextMethod.invoke(request);
      Context extractedContext =
          GlobalOpenTelemetry.getPropagators()
              .getTextMapPropagator()
              .extract(Context.root(), traceContext, GETTER);
      SpanContext spanContext = Span.fromContext(extractedContext).getSpanContext();

      return Context.current().with(Span.wrap(spanContext)).makeCurrent();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(@Advice.Enter @Nullable Scope scope) {
      if (scope != null) {
        scope.close();
      }
    }
  }
}
