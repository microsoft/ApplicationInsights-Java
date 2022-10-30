// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.google.auto.value.AutoValue;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import javax.annotation.Nullable;

/** Alert configuration for a given telemetry type. */
@AutoValue
public abstract class AlertConfiguration {

  public abstract AlertMetricType getType();

  public abstract boolean isEnabled();

  public abstract float getThreshold();

  public abstract int getProfileDurationSeconds();

  public abstract int getCooldownSeconds();

  @Nullable
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

    public abstract Builder setEnabled(boolean enabled);

    public abstract Builder setThreshold(float threshold);

    public abstract Builder setProfileDurationSeconds(int profileDurationSeconds);

    public abstract Builder setCooldownSeconds(int cooldownSeconds);

    public abstract Builder setType(AlertMetricType type);

    public abstract Builder setRequestTrigger(
        @Nullable AlertingConfig.RequestTrigger requestTrigger);

    public abstract AlertConfiguration build();
  }
}
