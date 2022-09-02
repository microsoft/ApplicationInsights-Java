// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.telemetry;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/** Superclass for all telemetry data classes. */
public abstract class BaseTelemetry implements Telemetry {

  private TelemetryContext context;
  private Date timestamp;

  protected BaseTelemetry() {}

  /** Initializes the instance with the context properties */
  protected void initialize(ConcurrentMap<String, String> properties) {
    context = new TelemetryContext(properties, new ContextTagsMap());
  }

  /** Gets date and time when event was recorded. */
  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  /** Sets date and time when event was recorded. */
  @Override
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  /** Gets the context associated with the current telemetry item. */
  @Override
  public TelemetryContext getContext() {
    return context;
  }

  /**
   * Gets a dictionary of application-defined property names and values providing additional
   * information about this event.
   */
  @Override
  public Map<String, String> getProperties() {
    return context.getProperties();
  }

  protected abstract Object getData();
}
