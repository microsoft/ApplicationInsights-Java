// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.functions.rpc.messages;

import java.util.Map;

public class RpcTraceContext {

  public Map<String, String> getAttributesMap() {
    throw new UnsupportedOperationException();
  }

  public String getTraceParent() {
    throw new UnsupportedOperationException();
  }

  public String getTraceState() {
    throw new UnsupportedOperationException();
  }
}
