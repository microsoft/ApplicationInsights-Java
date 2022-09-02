// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw;

public class ApplicationInsightsEtwException extends Exception {
  private static final long serialVersionUID = 6108441736100165651L;

  public ApplicationInsightsEtwException() {
    super();
  }

  public ApplicationInsightsEtwException(String message) {
    super(message);
  }

  public ApplicationInsightsEtwException(String message, Throwable cause) {
    super(message, cause);
  }
}
