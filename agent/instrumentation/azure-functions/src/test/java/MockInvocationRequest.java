// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.azure.functions.rpc.messages.InvocationRequest;
import com.microsoft.azure.functions.rpc.messages.RpcTraceContext;

class MockInvocationRequest extends InvocationRequest {

  private final RpcTraceContext traceContext;
  private final String invocationId;

  MockInvocationRequest(RpcTraceContext traceContext, String invocationId) {
    this.traceContext = traceContext;
    this.invocationId = invocationId;
  }

  @Override
  public RpcTraceContext getTraceContext() {
    return traceContext;
  }

  @Override
  public String getInvocationId() {
    return invocationId;
  }
}
