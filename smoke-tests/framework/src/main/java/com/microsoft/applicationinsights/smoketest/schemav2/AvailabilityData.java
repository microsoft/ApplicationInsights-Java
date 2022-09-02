// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AvailabilityData extends Domain {

  private String id;
  private String name;
  private Duration duration;
  private boolean success;
  private String runLocation;
  private String message;

  private ConcurrentMap<String, String> properties;
  private ConcurrentMap<String, Double> measurements;

  public AvailabilityData() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }

  public boolean getSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getRunLocation() {
    return runLocation;
  }

  public void setRunLocation(String runLocation) {
    this.runLocation = runLocation;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  /** Gets the Properties property. */
  public ConcurrentMap<String, String> getProperties() {
    if (this.properties == null) {
      this.properties = new ConcurrentHashMap<>();
    }
    return this.properties;
  }

  /** Gets the Measurements property. */
  public ConcurrentMap<String, Double> getMeasurements() {
    if (this.measurements == null) {
      this.measurements = new ConcurrentHashMap<>();
    }
    return this.measurements;
  }
}
