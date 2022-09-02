// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.AvailabilityData;
import java.util.concurrent.ConcurrentMap;

public final class AvailabilityTelemetry extends BaseTelemetry {

  private final AvailabilityData data;

  public AvailabilityTelemetry() {
    data = new AvailabilityData();
    initialize(data.getProperties());
  }

  public String getId() {
    return data.getId();
  }

  public void setId(String id) {
    data.setId(id);
  }

  public String getName() {
    return data.getName();
  }

  public void setName(String name) {
    data.setName(name);
  }

  public Duration getDuration() {
    return data.getDuration();
  }

  public void setDuration(Duration duration) {
    data.setDuration(duration);
  }

  public boolean getSuccess() {
    return data.getSuccess();
  }

  public void setSuccess(boolean success) {
    data.setSuccess(success);
  }

  public String getRunLocation() {
    return data.getRunLocation();
  }

  public void setRunLocation(String runLocation) {
    data.setRunLocation(runLocation);
  }

  public String getMessage() {
    return data.getMessage();
  }

  public void setMessage(String message) {
    data.setMessage(message);
  }

  /** Gets a dictionary of custom defined metrics. */
  public ConcurrentMap<String, Double> getMetrics() {
    return data.getMeasurements();
  }

  @Override
  protected AvailabilityData getData() {
    return data;
  }
}
