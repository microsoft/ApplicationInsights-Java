// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.applicationinsights.agent.bootstrap.AzureFunctions;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil;
import com.microsoft.applicationinsights.agent.bootstrap.BytecodeUtil.BytecodeUtilDelegate;
import com.microsoft.azure.functions.rpc.messages.InvocationRequest;
import com.microsoft.azure.functions.rpc.messages.RpcTraceContext;
import com.microsoft.azure.functions.worker.handler.InvocationRequestHandler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AzureFunctionsTest {

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  static {
    // this is needed since currently tests are run against otel javaagent, and not ai javaagent
    AzureFunctions.setup(() -> true, () -> {});
    BytecodeUtilDelegate delegate = mock(BytecodeUtilDelegate.class);
    when(delegate.shouldSample(anyString())).thenReturn(true);
    BytecodeUtil.setDelegate(delegate);
  }

  @Test
  void setRequestProperty() {
    // given
    String traceParent = "00-11111111111111111111111111111111-1111111111111111-00";
    String traceState = null;
    Map<String, String> attributesMap = emptyMap();
    RpcTraceContext traceContext = new MockRpcTraceContext(traceParent, traceState, attributesMap);

    String invocationId = null;
    InvocationRequest request = new MockInvocationRequest(traceContext, invocationId);

    AtomicReference<Context> contextRef = new AtomicReference<>();
    InvocationRequestHandler handler =
        new InvocationRequestHandler() {
          @Override
          protected void verifyCurrentContext() {
            contextRef.set(Context.current());
          }
        };

    // when
    handler.execute(request);

    // then
    Context context = contextRef.get();
    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    assertThat(spanContext.getTraceId()).isEqualTo("11111111111111111111111111111111");
    assertThat(spanContext.getSpanId()).isEqualTo("1111111111111111");
    assertThat(spanContext.getTraceFlags().isSampled()).isTrue();
  }
}
