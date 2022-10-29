// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DefaultConfiguration {

  public abstract boolean getSamplingEnabled();

  public abstract double getSamplingRate();

  public abstract long getSamplingProfileDuration();

  public static Builder builder() {
    return new AutoValue_DefaultConfiguration.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSamplingRate(double samplingRate);

    public abstract Builder setSamplingProfileDuration(long samplingProfileDuration);

    public abstract Builder setSamplingEnabled(boolean samplingEnabled);

    public abstract DefaultConfiguration build();
  }
}
