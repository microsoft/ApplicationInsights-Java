// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.functions.worker.handler;

import com.microsoft.azure.functions.rpc.messages.InvocationRequest;

public class InvocationRequestHandler {

  public void execute(InvocationRequest request) {
    verifyCurrentContext();
  }

  // this doesn't exist in the real worker artifact
  // it only exists for testing the instrumentation
  protected void verifyCurrentContext() {}
}
