// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

import javax.annotation.Nullable;

public class MachineNameFinder extends CachedDiagnosticsValueFinder {
  public static final String PROPERTY_NAME = "MachineName";

  @Override
  @Nullable
  protected String populateValue() {
    String computerName = System.getenv("COMPUTERNAME");
    if (computerName != null) {
      return computerName;
    }
    String hostname = System.getenv("HOSTNAME");
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
