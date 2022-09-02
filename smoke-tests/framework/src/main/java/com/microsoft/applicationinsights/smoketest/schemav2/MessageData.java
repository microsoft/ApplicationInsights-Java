// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Data contract class MessageData. */
public class MessageData extends Domain {
  /** Backing field for property Message. */
  private String message;

  /** Backing field for property SeverityLevel. */
  private SeverityLevel severityLevel;

  /** Backing field for property Properties. */
  private ConcurrentMap<String, String> properties;

  /** Initializes a new instance of the MessageData class. */
  public MessageData() {}

  /** Gets the Message property. */
  public String getMessage() {
    return this.message;
  }

  /** Sets the Message property. */
  public void setMessage(String value) {
    this.message = value;
  }

  /** Gets the SeverityLevel property. */
  public SeverityLevel getSeverityLevel() {
    return this.severityLevel;
  }

  /** Sets the SeverityLevel property. */
  public void setSeverityLevel(SeverityLevel value) {
    this.severityLevel = value;
  }

  /** Gets the Properties property. */
  public ConcurrentMap<String, String> getProperties() {
    if (this.properties == null) {
      this.properties = new ConcurrentHashMap<>();
    }
    return this.properties;
  }
}
