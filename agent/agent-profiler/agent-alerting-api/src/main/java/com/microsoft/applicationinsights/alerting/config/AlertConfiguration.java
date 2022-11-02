// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import javax.annotation.Nullable;

/** Alert configuration for a given telemetry type. */
@AutoValue
@JsonSerialize(as = AlertConfiguration.class)
@JsonDeserialize(builder = AlertConfiguration.Builder.class)
public abstract class AlertConfiguration {

  @JsonProperty("type")
  public abstract AlertMetricType getType();

  @JsonProperty("enabled")
  public abstract boolean isEnabled();

  @JsonProperty("threshold")
  public abstract float getThreshold();

  @JsonProperty("profileDuration")
  public abstract int getProfileDurationSeconds();

  @JsonProperty("cooldown")
  public abstract int getCooldownSeconds();

  @Nullable
  @JsonProperty("requestTrigger")
  public abstract AlertingConfig.RequestTrigger getRequestTrigger();

  public static Builder builder() {
    // TODO (trask) which of these is really required?
    return new AutoValue_AlertConfiguration.Builder()
        .setEnabled(false)
        .setThreshold(0)
        .setProfileDurationSeconds(0)
        .setCooldownSeconds(0);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @JsonCreator
    public static Builder builder() {
      return AlertConfiguration.builder();
    }

    @JsonProperty("enabled")
    public abstract Builder setEnabled(boolean enabled);

    @JsonProperty("threshold")
    public abstract Builder setThreshold(float threshold);

    @JsonProperty("profileDuration")
    public abstract Builder setProfileDurationSeconds(int profileDurationSeconds);

    @JsonProperty("cooldown")
    public abstract Builder setCooldownSeconds(int cooldownSeconds);

    @JsonProperty("type")
    public abstract Builder setType(AlertMetricType type);

    @JsonProperty("requestTrigger")
    public abstract Builder setRequestTrigger(
        @Nullable AlertingConfig.RequestTrigger requestTrigger);

    public abstract AlertConfiguration build();
  }
}
