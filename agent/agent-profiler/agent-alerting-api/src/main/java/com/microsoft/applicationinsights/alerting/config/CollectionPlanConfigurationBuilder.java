// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class CollectionPlanConfigurationBuilder {
  private boolean single = false;
  private EngineMode mode;
  private Instant expiration;
  private long immediateProfilingDuration;
  private String settingsMoniker;

  public CollectionPlanConfiguration createDefaultConfiguration() {
    return new CollectionPlanConfiguration(
        single, mode, expiration, immediateProfilingDuration, settingsMoniker);
  }

  public CollectionPlanConfigurationBuilder setCollectionPlanSingle(boolean single) {
    this.single = single;
    return this;
  }

  public CollectionPlanConfigurationBuilder setMode(EngineMode mode) {
    this.mode = mode;
    return this;
  }

  public CollectionPlanConfigurationBuilder setExpiration(long expiration) {
    this.expiration = parseBinaryDate(expiration);
    return this;
  }

  public static Instant parseBinaryDate(long expiration) {
    long ticks = expiration & 0x3fffffffffffffffL;
    long seconds = ticks / 10000000L;
    long nanos = (ticks % 10000000L) * 100L;
    long offset = OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
    return Instant.ofEpochSecond(seconds + offset, nanos);
  }

  public CollectionPlanConfigurationBuilder setImmediateProfilingDuration(
      long immediateProfilingDuration) {
    this.immediateProfilingDuration = immediateProfilingDuration;
    return this;
  }

  public CollectionPlanConfigurationBuilder setSettingsMoniker(String settingsMoniker) {
    this.settingsMoniker = settingsMoniker;
    return this;
  }
}
