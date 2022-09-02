// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.schemav2;

import com.microsoft.applicationinsights.telemetry.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RequestData {

  private String id;
  private Duration duration = new Duration(0);
  private String responseCode;
  private boolean success;
  private String source;
  private String name;
  private String url;

  private ConcurrentMap<String, String> properties;
  private ConcurrentMap<String, Double> measurements;

  public RequestData() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }

  public String getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(String responseCode) {
    this.responseCode = responseCode;
  }

  public boolean getSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
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
