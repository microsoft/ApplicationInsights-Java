// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.alert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.util.UUID;

/** Represents a breach of an alert threshold. */
@AutoValue
@JsonSerialize(as = AlertBreach.class)
@JsonDeserialize(builder = AlertBreach.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AlertBreach {

  @JsonProperty("type")
  public abstract AlertMetricType getType();

  // Value of the telemetry at the time of the breach
  @JsonProperty("alertValue")
  public abstract double getAlertValue();

  @JsonProperty("alertConfiguration")
  public abstract AlertConfiguration getAlertConfiguration();

  // CPU usage at the time of the breach
  @JsonProperty(value = "cpuMetric")
  public abstract double getCpuMetric();

  // MEMORY usage at the time of the breach
  @JsonProperty(value = "memoryUsage")
  public abstract double getMemoryUsage();

  // Unique ID for profile/breach
  @JsonProperty("profileId")
  public abstract String getProfileId();

  public abstract Builder toBuilder();

  public static AlertBreach.Builder builder() {
    return new AutoValue_AlertBreach.Builder()
        .setCpuMetric(0)
        .setMemoryUsage(0)
        .setProfileId(UUID.randomUUID().toString());
  }

  @AutoValue.Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public abstract static class Builder {

    @JsonCreator
    public static Builder builder() {
      return AlertBreach.builder();
    }

    @JsonProperty("type")
    public abstract Builder setType(AlertMetricType type);

    @JsonProperty("alertValue")
    public abstract Builder setAlertValue(double alertValue);

    @JsonProperty("alertConfiguration")
    public abstract Builder setAlertConfiguration(AlertConfiguration alertConfiguration);

    @JsonProperty(value = "cpuMetric")
    public abstract Builder setCpuMetric(double cpuMetric);

    @JsonProperty(value = "memoryUsage")
    public abstract Builder setMemoryUsage(double memoryUsage);

    @JsonProperty("profileId")
    public abstract Builder setProfileId(String profileId);

    public abstract AlertBreach build();
  }
}
