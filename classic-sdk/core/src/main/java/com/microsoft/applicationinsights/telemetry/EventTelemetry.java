// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.EventData;
import java.util.concurrent.ConcurrentMap;

/** Telemetry type used to track custom events in Azure Application Insights. */
public final class EventTelemetry extends BaseTelemetry {

  private final EventData data;

  /**
   * Creates a new instance.
   *
   * @param name The event's name. Max length 150.
   */
  public EventTelemetry(String name) {
    this();
    setName(name);
  }

  /** Creates a new instance. */
  public EventTelemetry() {
    data = new EventData();
    initialize(data.getProperties());
  }

  /** Gets the name of the event. */
  public String getName() {
    return data.getName();
  }

  /** Sets the name of the event. Max length 150. */
  public void setName(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("The event name cannot be null or empty");
    }
    data.setName(name);
  }

  /** Gets a dictionary of custom defined metrics. */
  public ConcurrentMap<String, Double> getMetrics() {
    return data.getMeasurements();
  }

  @Override
  protected EventData getData() {
    return data;
  }
}
