// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

public abstract class CachedDiagnosticsValueFinder implements DiagnosticsValueFinder {
  private volatile String value;

  @Override
  public String getValue() {
    if (value == null) {
      value = populateValue();
    }
    return value;
  }

  protected abstract String populateValue();
}
