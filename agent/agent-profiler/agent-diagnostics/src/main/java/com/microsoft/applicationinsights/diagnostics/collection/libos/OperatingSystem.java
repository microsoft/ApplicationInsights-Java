// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos;

public enum OperatingSystem {
  MAC_OS(false),
  LINUX(true),
  WINDOWS(false),
  SOLARIS(false),
  UNKNOWN(false);

  private final boolean supportsDiagnostics;

  OperatingSystem(boolean supportsDiagnostics) {
    this.supportsDiagnostics = supportsDiagnostics;
  }

  public boolean supportsDiagnostics() {
    return supportsDiagnostics;
  }
}
