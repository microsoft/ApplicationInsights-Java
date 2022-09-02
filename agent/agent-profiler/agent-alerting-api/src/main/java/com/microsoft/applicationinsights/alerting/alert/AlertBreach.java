// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.alert;

import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;

/** Represents a breach of an alert threshold. */
public class AlertBreach {
  private final AlertMetricType type;

  // Value of the telemetry at the time of the breach
  private final double alertValue;

  private final AlertConfiguration alertConfiguration;

  // CPU usage at the time of the breach
  private final double cpuUsage;

  // MEMORY usage at the time of the breach
  private final double memoryUsage;

  // Unique ID for profile/breach
  private final String profileId;

  public AlertBreach(
      AlertMetricType type,
      double alertValue,
      AlertConfiguration alertConfiguration,
      String profileId) {
    this(type, alertValue, alertConfiguration, profileId, 0, 0);
  }

  public AlertBreach(
      AlertMetricType type,
      double alertValue,
      AlertConfiguration alertConfiguration,
      String profileId,
      double cpuUsage,
      double memoryUsage) {
    this.type = type;
    this.alertValue = alertValue;
    this.alertConfiguration = alertConfiguration;
    this.cpuUsage = cpuUsage;
    this.memoryUsage = memoryUsage;
    this.profileId = profileId;
  }

  public AlertConfiguration getAlertConfiguration() {
    return alertConfiguration;
  }

  public double getAlertValue() {
    return alertValue;
  }

  public AlertMetricType getType() {
    return type;
  }

  public AlertBreach withCpuMetric(double cpuUsage) {
    return new AlertBreach(type, alertValue, alertConfiguration, profileId, cpuUsage, memoryUsage);
  }

  public AlertBreach withMemoryMetric(double memoryUsage) {
    return new AlertBreach(type, alertValue, alertConfiguration, profileId, cpuUsage, memoryUsage);
  }

  public String getTriggerName() {
    return "JFR-" + type.name();
  }

  public double getCpuMetric() {
    return cpuUsage;
  }

  public double getMemoryUsage() {
    return memoryUsage;
  }

  public String getProfileId() {
    return profileId;
  }
}
