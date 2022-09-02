// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import java.time.Instant;
import java.util.Objects;
import javax.annotation.Nullable;

public class CollectionPlanConfiguration {

  private final boolean single;
  private final EngineMode mode;
  private final Instant expiration;
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
      Instant expiration,
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

  public Instant getExpiration() {
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
