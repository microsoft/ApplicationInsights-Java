// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.data;

import com.google.auto.value.AutoValue;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.time.Instant;

/** Individual sample of telemetry data. */
@AutoValue
public abstract class TelemetryDataPoint {

  public static TelemetryDataPoint create(
      AlertMetricType type, Instant time, String name, double value) {
    return new AutoValue_TelemetryDataPoint(type, time, name, value);
  }

  public abstract AlertMetricType getType();

  public abstract Instant getTime();

  public abstract String getName();

  public abstract double getValue();
}
