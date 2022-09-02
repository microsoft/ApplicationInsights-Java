// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import java.util.Objects;
import javax.annotation.Nullable;

public class DefaultConfiguration {

  private final boolean samplingEnabled;
  private final double samplingRate;
  private final long samplingProfileDuration;

  public DefaultConfiguration(
      boolean samplingEnabled, double samplingRate, long samplingProfileDuration) {
    this.samplingEnabled = samplingEnabled;
    this.samplingRate = samplingRate;
    this.samplingProfileDuration = samplingProfileDuration;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof DefaultConfiguration)) {
      return false;
    }
    DefaultConfiguration that = (DefaultConfiguration) obj;
    return samplingEnabled == that.samplingEnabled
        && samplingRate == that.samplingRate
        && samplingProfileDuration == that.samplingProfileDuration;
  }

  @Override
  public int hashCode() {
    return Objects.hash(samplingEnabled, samplingRate, samplingProfileDuration);
  }

  public double getSamplingRate() {
    return samplingRate;
  }

  public long getSamplingProfileDuration() {
    return samplingProfileDuration;
  }

  public boolean getSamplingEnabled() {
    return samplingEnabled;
  }
}
