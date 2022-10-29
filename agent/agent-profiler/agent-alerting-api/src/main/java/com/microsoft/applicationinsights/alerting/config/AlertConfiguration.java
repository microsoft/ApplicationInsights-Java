package com.microsoft.applicationinsights.alerting.config;

import com.google.auto.value.AutoValue;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import javax.annotation.Nullable;

/** Alert configuration for a given telemetry type. */
@AutoValue
public abstract class AlertConfiguration {

  public static AlertConfiguration create(
      AlertMetricType type, boolean enabled, float threshold, long profileDuration, long cooldown) {

    return builder()
        .setType(type)
        .setEnabled(enabled)
        .setThreshold(threshold)
        .setProfileDuration(profileDuration)
        .setCooldown(cooldown)
        .build();
  }

  public static AlertConfiguration create(
      AlertMetricType type,
      boolean enabled,
      float threshold,
      long profileDuration,
      long cooldown,
      @Nullable AlertingConfig.RequestTrigger requestTrigger) {

    return builder()
        .setType(type)
        .setEnabled(enabled)
        .setThreshold(threshold)
        .setProfileDuration(profileDuration)
        .setCooldown(cooldown)
        .setRequestTrigger(requestTrigger)
        .build();
  }

  public abstract AlertMetricType getType();

  public abstract boolean isEnabled();

  public abstract float getThreshold();

  public abstract long getProfileDuration();

  public abstract long getCooldown();

  @Nullable
  public abstract AlertingConfig.RequestTrigger getRequestTrigger();

  public static Builder builder() {
    return new AutoValue_AlertConfiguration.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setEnabled(boolean enabled);

    public abstract Builder setThreshold(float threshold);

    public abstract Builder setProfileDuration(long profileDuration);

    public abstract Builder setCooldown(long cooldown);

    public abstract Builder setType(AlertMetricType type);

    public abstract Builder setRequestTrigger(
        @Nullable AlertingConfig.RequestTrigger requestTrigger);

    public abstract AlertConfiguration build();
  }
}
