// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.alert;

import com.google.auto.value.AutoValue;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;

/** Represents a breach of an alert threshold. */
@AutoValue
public abstract class AlertBreach {

  public abstract AlertMetricType getType();

  // Value of the telemetry at the time of the breach
  public abstract double getAlertValue();

  public abstract AlertConfiguration getAlertConfiguration();

  // CPU usage at the time of the breach
  public abstract double getCpuMetric();

  // MEMORY usage at the time of the breach
  public abstract double getMemoryUsage();

  // Unique ID for profile/breach
  public abstract String getProfileId();

  public abstract Builder toBuilder();

  public static AlertBreach.Builder builder() {
    return new AutoValue_AlertBreach.Builder().setCpuMetric(0).setMemoryUsage(0);
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setType(AlertMetricType type);

    public abstract Builder setAlertValue(double alertValue);

    public abstract Builder setAlertConfiguration(AlertConfiguration alertConfiguration);

    public abstract Builder setCpuMetric(double cpuMetric);

    public abstract Builder setMemoryUsage(double memoryUsage);

    public abstract Builder setProfileId(String profileId);

    public abstract AlertBreach build();
  }
}
