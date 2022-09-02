// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.schemav2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExceptionData {

  private SeverityLevel severityLevel;

  private ConcurrentMap<String, String> properties;
  private ConcurrentMap<String, Double> measurements;

  public ExceptionData() {}

  public SeverityLevel getSeverityLevel() {
    return severityLevel;
  }

  public void setSeverityLevel(SeverityLevel severityLevel) {
    this.severityLevel = severityLevel;
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
