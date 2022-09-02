// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.internal.schemav2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MessageData {

  private String message;
  private SeverityLevel severityLevel;

  private ConcurrentMap<String, String> properties;

  public MessageData() {}

  public String getMessage() {
    return message;
  }

  public void setMessage(String value) {
    this.message = value;
  }

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
}
