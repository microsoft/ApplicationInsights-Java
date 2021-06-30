/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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

  public AlertBreach(
      AlertMetricType type, double alertValue, AlertConfiguration alertConfiguration) {
    this(type, alertValue, alertConfiguration, 0, 0);
  }

  public AlertBreach(
      AlertMetricType type,
      double alertValue,
      AlertConfiguration alertConfiguration,
      double cpuUsage,
      double memoryUsage) {
    this.type = type;
    this.alertValue = alertValue;
    this.alertConfiguration = alertConfiguration;
    this.cpuUsage = cpuUsage;
    this.memoryUsage = memoryUsage;
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
    return new AlertBreach(type, alertValue, alertConfiguration, cpuUsage, memoryUsage);
  }

  public AlertBreach withMemoryMetric(double memoryUsage) {
    return new AlertBreach(type, alertValue, alertConfiguration, cpuUsage, memoryUsage);
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
}
