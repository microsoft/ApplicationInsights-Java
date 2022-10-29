// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.google.auto.value.AutoValue;
import java.time.Instant;

@AutoValue
public abstract class CollectionPlanConfiguration {

  public abstract boolean isSingle();

  public abstract EngineMode getMode();

  public abstract Instant getExpiration();

  public abstract int getImmediateProfilingDurationSeconds();

  public abstract String getSettingsMoniker();

  public static CollectionPlanConfiguration.Builder builder() {
    return new AutoValue_CollectionPlanConfiguration.Builder();
  }

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

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract CollectionPlanConfiguration.Builder setSingle(boolean single);

    public abstract CollectionPlanConfiguration.Builder setMode(EngineMode mode);

    public abstract CollectionPlanConfiguration.Builder setExpiration(Instant expiration);

    public abstract CollectionPlanConfiguration.Builder setImmediateProfilingDurationSeconds(
        int immediateProfilingDurationSeconds);

    public abstract CollectionPlanConfiguration.Builder setSettingsMoniker(String settingsMoniker);

    public abstract CollectionPlanConfiguration build();
  }
}
