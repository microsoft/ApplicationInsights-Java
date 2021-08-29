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

import java.time.ZonedDateTime;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CollectionPlanConfiguration {

  private final boolean single;
  private final EngineMode mode;
  private final ZonedDateTime expiration;
  private final long immediateProfilingDuration;
  private final String settingsMoniker;

  public enum EngineMode {
    unknown,
    standby,
    sampling,
    immediate;

    public static EngineMode parse(String value) {
      try {
        return EngineMode.valueOf(value.toLowerCase());
      } catch (IllegalArgumentException e) {
        return unknown;
      }
    }
  }

  public CollectionPlanConfiguration(
      boolean single,
      EngineMode mode,
      ZonedDateTime expiration,
      long immediateProfilingDuration,
      String settingsMoniker) {
    this.single = single;
    this.mode = mode;
    this.expiration = expiration;
    this.immediateProfilingDuration = immediateProfilingDuration;
    this.settingsMoniker = settingsMoniker;
  }

  public boolean isSingle() {
    return single;
  }

  public EngineMode getMode() {
    return mode;
  }

  public ZonedDateTime getExpiration() {
    return expiration;
  }

  public long getImmediateProfilingDuration() {
    return immediateProfilingDuration;
  }

  public String getSettingsMoniker() {
    return settingsMoniker;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof CollectionPlanConfiguration)) {
      return false;
    }
    CollectionPlanConfiguration that = (CollectionPlanConfiguration) obj;
    return single == that.single
        && expiration.equals(that.expiration)
        && immediateProfilingDuration == that.immediateProfilingDuration
        && Objects.equals(mode, that.mode)
        && Objects.equals(settingsMoniker, that.settingsMoniker);
  }

  @Override
  public int hashCode() {
    return Objects.hash(single, mode, expiration, immediateProfilingDuration, settingsMoniker);
  }
}
