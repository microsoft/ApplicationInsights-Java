// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.schemav2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EventData {

  private String name;

  private ConcurrentMap<String, String> properties;
  private ConcurrentMap<String, Double> measurements;

  public EventData() {}

  public String getName() {
    return name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public ConcurrentMap<String, String> getProperties() {
    if (properties == null) {
      properties = new ConcurrentHashMap<>();
    }
    return properties;
  }

  public void setProperties(ConcurrentMap<String, String> properties) {
    this.properties = properties;
  }

  public ConcurrentMap<String, Double> getMeasurements() {
    if (measurements == null) {
      measurements = new ConcurrentHashMap<>();
    }
    return measurements;
  }

  public void setMeasurements(ConcurrentMap<String, Double> measurements) {
    this.measurements = measurements;
  }
}
