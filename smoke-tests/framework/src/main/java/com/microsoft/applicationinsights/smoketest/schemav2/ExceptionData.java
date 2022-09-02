// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Data contract class ExceptionData. */
public class ExceptionData extends Domain {
  /** Backing field for property Exceptions. */
  private List<ExceptionDetails> exceptions;

  /** Backing field for property SeverityLevel. */
  private SeverityLevel severityLevel;

  /** Backing field for property Properties. */
  private ConcurrentMap<String, String> properties;

  /** Backing field for property Measurements. */
  private ConcurrentMap<String, Double> measurements;

  /** Initializes a new instance of the ExceptionData class. */
  public ExceptionData() {}

  /** Gets the Exceptions property. */
  public List<ExceptionDetails> getExceptions() {
    if (this.exceptions == null) {
      this.exceptions = new ArrayList<>();
    }
    return this.exceptions;
  }

  /** Sets the Exceptions property. */
  public void setExceptions(List<ExceptionDetails> value) {
    this.exceptions = value;
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

  /** Gets the Measurements property. */
  public ConcurrentMap<String, Double> getMeasurements() {
    if (this.measurements == null) {
      this.measurements = new ConcurrentHashMap<>();
    }
    return this.measurements;
  }
}
