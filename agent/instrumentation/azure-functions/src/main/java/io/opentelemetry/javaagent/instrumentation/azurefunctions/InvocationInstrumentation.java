// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package io.opentelemetry.javaagent.instrumentation.azurefunctions;

import static io.opentelemetry.javaagent.instrumentation.azurefunctions.InvocationRequestExtractAdapter.GETTER;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.microsoft.applicationinsights.agent.bootstrap.AzureFunctions;
import com.microsoft.applicationinsights.agent.bootstrap.AzureFunctionsCustomDimensions;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil;
import com.microsoft.azure.functions.rpc.messages.InvocationRequest;
import com.microsoft.azure.functions.rpc.messages.RpcTraceContext;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Map;
import javax.annotation.Nullable;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

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

  @SuppressWarnings({"unused", "PrivateConstructorForUtilityClass", "MustBeClosedChecker"})
  public static class ExecuteAdvice {
    @Nullable
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Scope methodEnter(@Advice.Argument(0) InvocationRequest request) {

      if (!AzureFunctions.hasConnectionString()) {
        return null;
      }

      RpcTraceContext traceContext = request.getTraceContext();
      Context extractedContext =
          GlobalOpenTelemetry.getPropagators()
              .getTextMapPropagator()
              .extract(Context.root(), traceContext, GETTER);
      SpanContext spanContext = Span.fromContext(extractedContext).getSpanContext();

      // recreate SpanContext to override the trace flags since the host currently always sends "00"
      TraceFlags traceFlags =
          BytecodeUtil.shouldSample(spanContext.getTraceId())
              ? TraceFlags.getSampled()
              : TraceFlags.getDefault();
      spanContext =
          SpanContext.createFromRemoteParent(
              spanContext.getTraceId(),
              spanContext.getSpanId(),
              traceFlags,
              spanContext.getTraceState());

      Map<String, String> attributesMap = traceContext.getAttributesMap();
      AzureFunctionsCustomDimensions customDimensions =
          new AzureFunctionsCustomDimensions(
              request.getInvocationId(),
              attributesMap.get("ProcessId"),
              attributesMap.get("LogLevel"),
              attributesMap.get("Category"),
              attributesMap.get("HostInstanceId"),
              attributesMap.get("#AzFuncLiveLogsSessionId"));

      return Context.current().with(Span.wrap(spanContext)).with(customDimensions).makeCurrent();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(@Advice.Enter @Nullable Scope scope) {
      if (scope != null) {
        scope.close();
      }
    }
  }
}
