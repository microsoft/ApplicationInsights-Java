// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

public class DefaultConfigurationBuilder {
  private boolean samplingEnabled;
  private double samplingRate;
  private long samplingProfileDuration;

  public DefaultConfigurationBuilder setSamplingEnabled(boolean samplingEnabled) {
    this.samplingEnabled = samplingEnabled;
    return this;
  }

  public DefaultConfigurationBuilder setSamplingRate(double samplingRate) {
    this.samplingRate = samplingRate;
    return this;
  }

  public DefaultConfigurationBuilder setSamplingProfileDuration(long samplingProfileDuration) {
    this.samplingProfileDuration = samplingProfileDuration;
    return this;
  }

  public DefaultConfiguration createDefaultConfiguration() {
    return new DefaultConfiguration(samplingEnabled, samplingRate, samplingProfileDuration);
  }
}
