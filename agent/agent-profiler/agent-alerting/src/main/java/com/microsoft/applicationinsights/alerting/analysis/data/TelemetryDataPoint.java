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

package com.microsoft.applicationinsights.alerting.analysis.data;

import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.time.Instant;
import java.util.Objects;
import javax.annotation.Nullable;

/** Individual sample of telemetry data. */
public class TelemetryDataPoint implements Comparable<TelemetryDataPoint> {
  private final AlertMetricType type;
  private final Instant time;
  private final double value;
  private final String name;

  public TelemetryDataPoint(AlertMetricType type, Instant time, String name, double value) {
    this.type = type;
    this.time = time;
    this.value = value;
    this.name = name;
  }

  public double getValue() {
    return value;
  }

  public String getName() {
    return name;
  }

  public Instant getTime() {
    return time;
  }

  /** Sort first by timestamp, then value, then type. */
  @Override
  public int compareTo(TelemetryDataPoint telemetryDataPoint) {
    if (!time.equals(telemetryDataPoint.time)) {
      return time.compareTo(telemetryDataPoint.time);
    } else if (value != telemetryDataPoint.getValue()) {
      return Double.compare(value, telemetryDataPoint.value);
    } else if (!name.equals(telemetryDataPoint.getName())) {
      return name.compareTo(telemetryDataPoint.getName());
    } else {
      return type.compareTo(telemetryDataPoint.type);
    }
  }

  public AlertMetricType getType() {
    return type;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof TelemetryDataPoint)) {
      return false;
    }
    TelemetryDataPoint telemetryDataPoint = (TelemetryDataPoint) obj;
    return Double.compare(telemetryDataPoint.value, value) == 0
        && type == telemetryDataPoint.type
        && name.equals(telemetryDataPoint.getName())
        && Objects.equals(time, telemetryDataPoint.time);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, time, value, name);
  }
}
