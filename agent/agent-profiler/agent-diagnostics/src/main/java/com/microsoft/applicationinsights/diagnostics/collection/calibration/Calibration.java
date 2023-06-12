// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.calibration;

public class Calibration {
  public static final double UNKNOWN = -1;
  private final double contextSwitchingRate;

  public Calibration(double contextSwitchingRate) {
    this.contextSwitchingRate = contextSwitchingRate;
  }

  public double getContextSwitchingRate() {
    return contextSwitchingRate;
  }
}
