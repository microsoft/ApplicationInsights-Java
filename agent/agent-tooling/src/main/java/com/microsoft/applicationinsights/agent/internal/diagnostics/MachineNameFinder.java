// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.diagnostics;

import java.util.function.Function;
import javax.annotation.Nullable;

public class MachineNameFinder extends CachedDiagnosticsValueFinder {
  public static final String PROPERTY_NAME = "MachineName";

  @Override
  @Nullable
  protected String populateValue(Function<String, String> envVarsFunction) {
    String computerName = envVarsFunction.apply("COMPUTERNAME");
    if (computerName != null) {
      return computerName;
    }
    String hostname = envVarsFunction.apply("HOSTNAME");
    if (hostname != null) {
      return hostname;
    }
    return null;
  }

  @Override
  public String getName() {
    return PROPERTY_NAME;
  }
}
