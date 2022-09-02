// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.schemav2;

import com.microsoft.applicationinsights.telemetry.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteDependencyData {

  private String name;
  private String id;
  private String resultCode;
  private Duration duration = new Duration(0);
  private Boolean success = true;
  private String data;
  private String type;
  private String target;

  private ConcurrentMap<String, String> properties;
  private ConcurrentMap<String, Double> measurements;

  public RemoteDependencyData() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getResultCode() {
    return resultCode;
  }

  public void setResultCode(String resultCode) {
    this.resultCode = resultCode;
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
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
