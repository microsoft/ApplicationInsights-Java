// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis.aggregations;

import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import java.util.OptionalDouble;
import java.util.function.DoubleConsumer;
import javax.annotation.Nullable;

/** A process that consumes data points and computes metrics. */
public abstract class Aggregation {
  @Nullable protected DoubleConsumer consumer = null;

  /** Add new data to the aggregation. */
  public void update(TelemetryDataPoint telemetryDataPoint) {
    processUpdate(telemetryDataPoint);
    OptionalDouble value = compute();
    if (value.isPresent() && consumer != null) {
      consumer.accept(value.getAsDouble());
    }
  }

  protected abstract void processUpdate(TelemetryDataPoint telemetryDataPoint);

  /** Add a consumer that is notified when new aggregated data is available. */
  public void setConsumer(DoubleConsumer consumer) {
    this.consumer = consumer;
  }

  /** Compute the current aggregation of the data. */
  public abstract OptionalDouble compute();
}
