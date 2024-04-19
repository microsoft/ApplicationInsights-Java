// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics;

import java.util.function.Function;

public abstract class CachedDiagnosticsValueFinder implements DiagnosticsValueFinder {
  private volatile String value;

  @Override
  public String getValue(Function<String, String> envVarsFunction) {
    if (value == null) {
      value = populateValue(envVarsFunction);
    }
    return value;
  }

  protected abstract String populateValue(Function<String, String> envVarsFunction);
}
