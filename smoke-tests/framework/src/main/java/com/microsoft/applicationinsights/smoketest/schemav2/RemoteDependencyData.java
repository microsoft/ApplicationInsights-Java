/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
