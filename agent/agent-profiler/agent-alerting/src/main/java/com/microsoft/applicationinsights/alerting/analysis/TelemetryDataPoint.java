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

package com.microsoft.applicationinsights.alerting.analysis;

import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Individual sample of telemetry data. */
public class TelemetryDataPoint implements Comparable<TelemetryDataPoint> {
  private final AlertMetricType type;
  private final ZonedDateTime time;
  private final double value;

  public TelemetryDataPoint(AlertMetricType type, ZonedDateTime time, double value) {
    this.type = type;
    this.time = time;
    this.value = value;
  }

  public double getValue() {
    return value;
  }

  public ZonedDateTime getTime() {
    return time;
  }

  /** Sort first by timestamp, then value, then type. */
  @Override
  public int compareTo(TelemetryDataPoint telemetryDataPoint) {
    if (!time.equals(telemetryDataPoint.time)) {
      return time.compareTo(telemetryDataPoint.time);
    } else if (value != telemetryDataPoint.getValue()) {
      return Double.compare(value, telemetryDataPoint.value);
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
        && Objects.equals(time, telemetryDataPoint.time);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, time, value);
  }
}
