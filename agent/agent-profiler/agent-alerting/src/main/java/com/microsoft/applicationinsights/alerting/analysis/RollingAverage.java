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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Consumer;

/** Applies a time window to data and calculates a mean of the data during that window */
public class RollingAverage {

  private final long windowLengthInSec;
  private final TimeSource timeSource;
  private final List<TelemetryDataPoint> telemetryDataPoints = new ArrayList<>();
  private Consumer<Double> consumer;
  private static final int DEFAULT_ROLLING_AVERAGE_WINDOW_IN_SEC =
      Integer.parseInt(
          System.getProperty(
              "applicationinsights.preview.profiler.rolling-average-window-in-sec", "120"));

  public RollingAverage() {
    windowLengthInSec = DEFAULT_ROLLING_AVERAGE_WINDOW_IN_SEC;
    timeSource = TimeSource.DEFAULT;
  }

  public RollingAverage(long windowLengthInSec, TimeSource timeSource) {
    this.windowLengthInSec = windowLengthInSec;
    this.timeSource = timeSource;
  }

  public long getWindowLengthInSec() {
    return windowLengthInSec;
  }

  public RollingAverage setConsumer(Consumer<Double> consumer) {
    this.consumer = consumer;
    return this;
  }

  public double track(TelemetryDataPoint telemetryDataPoint) {
    ZonedDateTime now = timeSource.getNow();
    telemetryDataPoints.add(telemetryDataPoint);

    removeOldValues(now);

    OptionalDouble average = calculateAverage();
    if (average.isPresent()) {
      consumer.accept(average.getAsDouble());
      return average.getAsDouble();
    } else {
      return 0.0d;
    }
  }

  public OptionalDouble calculateAverage() {
    return telemetryDataPoints.stream().mapToDouble(TelemetryDataPoint::getValue).average();
  }

  private void removeOldValues(ZonedDateTime now) {
    ZonedDateTime cutOff = now.minusSeconds(windowLengthInSec);

    // Ensure that we keep at least 2 values in our buffer so that we are not reacting to a single
    // value
    while (telemetryDataPoints.size() > 2
        && telemetryDataPoints.get(0).getTime().isBefore(cutOff)) {
      telemetryDataPoints.remove(0);
    }
  }
}
