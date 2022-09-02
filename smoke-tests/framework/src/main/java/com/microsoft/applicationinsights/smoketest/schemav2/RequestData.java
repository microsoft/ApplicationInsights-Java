// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest.schemav2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Data contract class RequestData. */
public class RequestData extends Domain {
  /** Backing field for property Id. */
  private String id;

  /** Backing field for property Duration. */
  private Duration duration = new Duration(0);

  /** Backing field for property ResponseCode. */
  private String responseCode;

  /** Backing field for property Success. */
  private boolean success;

  /** Backing field for property Source. */
  private String source;

  /** Backing field for property Name. */
  private String name;

  /** Backing field for property Url. */
  private String url;

  /** Backing field for property Properties. */
  private ConcurrentMap<String, String> properties;

  /** Backing field for property Measurements. */
  private ConcurrentMap<String, Double> measurements;

  /** Initializes a new instance of the RequestData class. */
  public RequestData() {}

  /** Gets the Id property. */
  public String getId() {
    return this.id;
  }

  /** Sets the Id property. */
  public void setId(String value) {
    this.id = value;
  }

  /** Gets the Duration property. */
  public Duration getDuration() {
    return this.duration;
  }

  /** Sets the Duration property. */
  public void setDuration(Duration value) {
    this.duration = value;
  }

  /** Gets the ResponseCode property. */
  public String getResponseCode() {
    return this.responseCode;
  }

  /** Sets the ResponseCode property. */
  public void setResponseCode(String value) {
    this.responseCode = value;
  }

  /** Gets the Success property. */
  public boolean getSuccess() {
    return this.success;
  }

  /** Sets the Success property. */
  public void setSuccess(boolean value) {
    this.success = value;
  }

  /** Gets the Source property. */
  public String getSource() {
    return this.source;
  }

  /** Sets the Source property. */
  public void setSource(String value) {
    this.source = value;
  }

  /** Gets the Name property. */
  public String getName() {
    return this.name;
  }

  /** Sets the Name property. */
  public void setName(String value) {
    this.name = value;
  }

  /** Gets the Url property. */
  public String getUrl() {
    return this.url;
  }

  /** Sets the Url property. */
  public void setUrl(String value) {
    this.url = value;
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
