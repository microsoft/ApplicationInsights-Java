/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.alerting.config;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

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
