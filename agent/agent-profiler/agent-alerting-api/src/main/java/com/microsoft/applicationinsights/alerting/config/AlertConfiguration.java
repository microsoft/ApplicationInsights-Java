// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.azure.json.JsonSerializable;
import com.azure.json.JsonWriter;
import com.google.auto.value.AutoValue;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.io.IOException;
import javax.annotation.Nullable;

/** Alert configuration for a given telemetry type. */
@AutoValue
public abstract class AlertConfiguration implements JsonSerializable<AlertConfiguration> {

  protected AlertMetricType type;
  protected boolean enabled;
  protected float threshold;
  protected int profileDurationSeconds;
  protected int cooldownSeconds;
  protected AlertingConfig.RequestTrigger requestTrigger;

  public abstract AlertMetricType getType();

  public AlertConfiguration setType(AlertMetricType type) {
    this.type = type;
    return this;
  }

  public abstract boolean isEnabled();

  public AlertConfiguration setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public abstract float getThreshold();

  public AlertConfiguration setThreshold(float threshold) {
    this.threshold = threshold;
    return this;
  }

  public abstract int getProfileDurationSeconds();

  public AlertConfiguration setProfileDurationSeconds(int profileDurationSeconds) {
    this.profileDurationSeconds = profileDurationSeconds;
    return this;
  }

  public abstract int getCooldownSeconds();

  public AlertConfiguration setCooldownSeconds(int cooldownSeconds) {
    this.cooldownSeconds = cooldownSeconds;
    return this;
  }

  @Nullable
  public abstract AlertingConfig.RequestTrigger getRequestTrigger();

  public AlertConfiguration setRequestTrigger(AlertingConfig.RequestTrigger requestTrigger) {
    this.requestTrigger = requestTrigger;
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    jsonWriter.writeStartObject();
    if (type != null) {
      jsonWriter.writeStringField("type", type.name());
    }
    jsonWriter.writeBooleanField("enabled", enabled);
    jsonWriter.writeFloatField("threshold", threshold);
    jsonWriter.writeIntField("profileDurationSeconds", profileDurationSeconds);
    jsonWriter.writeIntField("cooldownSeconds", cooldownSeconds);
    jsonWriter.writeJsonField("requestTrigger", requestTrigger);
    jsonWriter.writeEndObject();
    return jsonWriter;
  }

  public static Builder builder() {
    // TODO (trask) which of these is really required?
    return new AutoValue_AlertConfiguration.Builder()
        .setEnabled(false)
        .setThreshold(0)
        .setProfileDurationSeconds(0)
        .setCooldownSeconds(0);
  }

  @AutoValue.Builder
  public abstract static class Builder implements JsonSerializable<Builder> {
    private AlertMetricType type;
    private boolean enabled;
    private float threshold;
    private int profileDurationSeconds;
    private int cooldownSeconds;
    private AlertingConfig.RequestTrigger requestTrigger;

    public abstract Builder setEnabled(boolean enabled);

    public abstract Builder setThreshold(float threshold);

    public abstract Builder setProfileDurationSeconds(int profileDurationSeconds);

    public abstract Builder setCooldownSeconds(int cooldownSeconds);

    public abstract Builder setType(AlertMetricType type);

    public abstract Builder setRequestTrigger(
        @Nullable AlertingConfig.RequestTrigger requestTrigger);

    public abstract AlertConfiguration build();

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      if (type != null) {
        jsonWriter.writeStringField("type", type.name());
      }
      jsonWriter.writeBooleanField("enabled", enabled);
      jsonWriter.writeFloatField("threshold", threshold);
      jsonWriter.writeIntField("profileDurationSeconds", profileDurationSeconds);
      jsonWriter.writeIntField("cooldownSeconds", cooldownSeconds);
      jsonWriter.writeJsonField("requestTrigger", requestTrigger);
      jsonWriter.writeEndObject();
      return jsonWriter;
    }
  }
}
