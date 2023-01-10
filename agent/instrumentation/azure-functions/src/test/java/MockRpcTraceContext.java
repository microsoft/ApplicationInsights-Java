// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.azure.functions.rpc.messages.RpcTraceContext;
import java.util.Map;

class MockRpcTraceContext extends RpcTraceContext {

  private final String traceParent;
  private final String traceState;
  private final Map<String, String> attributesMap;

  MockRpcTraceContext(String traceParent, String traceState, Map<String, String> attributesMap) {
    this.traceParent = traceParent;
    this.traceState = traceState;
    this.attributesMap = attributesMap;
  }

  @Override
  public String getTraceParent() {
    return traceParent;
  }

  @Override
  public String getTraceState() {
    return traceState;
  }

  @Override
  public Map<String, String> getAttributesMap() {
    return attributesMap;
  }
}
