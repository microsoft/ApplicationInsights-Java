// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Data contract class RemoteDependencyData. */
public class RemoteDependencyData extends Domain {
  /** Backing field for property Name. */
  private String name;

  /** Backing field for property Id. */
  private String id;

  /** Backing field for property ResultCode. */
  private String resultCode;

  /** Backing field for property Duration. */
  private Duration duration = new Duration(0);

  /** Backing field for property Success. */
  private Boolean success = true;

  /** Backing field for property Data. */
  private String data;

  /** Backing field for property Type. */
  private String type;

  /** Backing field for property Target. */
  private String target;

  /** Backing field for property Properties. */
  private ConcurrentMap<String, String> properties;

  /** Backing field for property Measurements. */
  private ConcurrentMap<String, Double> measurements;

  /** Initializes a new instance of the RemoteDependencyData class. */
  public RemoteDependencyData() {}

  /** Gets the Name property. */
  public String getName() {
    return this.name;
  }

  /** Sets the Name property. */
  public void setName(String value) {
    this.name = value;
  }

  /** Gets the Id property. */
  public String getId() {
    return this.id;
  }

  /** Sets the Id property. */
  public void setId(String value) {
    this.id = value;
  }

  /** Gets the ResultCode property. */
  public String getResultCode() {
    return this.resultCode;
  }

  /** Sets the ResultCode property. */
  public void setResultCode(String value) {
    this.resultCode = value;
  }

  /** Gets the Duration property. */
  public Duration getDuration() {
    return this.duration;
  }

  /** Sets the Duration property. */
  public void setDuration(Duration value) {
    this.duration = value;
  }

  /** Gets the Success property. */
  public Boolean getSuccess() {
    return this.success;
  }

  /** Sets the Success property. */
  public void setSuccess(Boolean value) {
    this.success = value;
  }

  /** Gets the Data property. */
  public String getData() {
    return this.data;
  }

  /** Sets the Data property. */
  public void setData(String value) {
    this.data = value;
  }

  /** Gets the Type property. */
  public String getType() {
    return this.type;
  }

  /** Sets the Type property. */
  public void setType(String value) {
    this.type = value;
  }

  /** Gets the Target property. */
  public String getTarget() {
    return this.target;
  }

  /** Sets the Target property. */
  public void setTarget(String value) {
    this.target = value;
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
