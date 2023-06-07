// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.jfr;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;

@SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "Java8ApiChecker"})
@Name("com.microsoft.applicationinsights.diagnostics.jfr.MachineStats")
@Label("MachineStats")
@Category("Diagnostic")
@Description("MachineStats")
@StackTrace(false)
@Period("beginChunk")
public class MachineStats extends Event {
  public static final String NAME =
      "com.microsoft.applicationinsights.diagnostics.jfr.MachineStats";
  private final double contextSwitchesPerMs;

  private final int coreCount;

  public MachineStats(double contextSwitchesPerMs, int coreCount) {
    this.contextSwitchesPerMs = contextSwitchesPerMs;
    this.coreCount = coreCount;
  }

  public double getContextSwitchesPerMs() {
    return contextSwitchesPerMs;
  }

  public int getCoreCount() {
    return coreCount;
  }
}
