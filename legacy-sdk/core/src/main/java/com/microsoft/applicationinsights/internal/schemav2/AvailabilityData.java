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

package com.microsoft.applicationinsights.internal.schemav2;

import com.microsoft.applicationinsights.telemetry.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AvailabilityData {

  private String id;
  private String name;
  private Duration duration = new Duration(0);
  private boolean success;
  private String runLocation;
  private String message;

  private ConcurrentMap<String, String> properties;
  private ConcurrentMap<String, Double> measurements;

  public AvailabilityData() {}

  public String getId() {
    return this.id;
  }

  public void setId(String value) {
    this.id = value;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String value) {
    this.name = value;
  }

  public Duration getDuration() {
    return this.duration;
  }

  public void setDuration(Duration value) {
    this.duration = value;
  }

  public boolean getSuccess() {
    return this.success;
  }

  public void setSuccess(boolean value) {
    this.success = value;
  }

  public String getRunLocation() {
    return this.runLocation;
  }

  public void setRunLocation(String value) {
    this.runLocation = value;
  }

  public String getMessage() {
    return this.message;
  }

  public void setMessage(String value) {
    this.message = value;
  }

  public ConcurrentMap<String, String> getProperties() {
    if (this.properties == null) {
      this.properties = new ConcurrentHashMap<>();
    }
    return this.properties;
  }

  public void setProperties(ConcurrentMap<String, String> value) {
    this.properties = value;
  }

  public ConcurrentMap<String, Double> getMeasurements() {
    if (this.measurements == null) {
      this.measurements = new ConcurrentHashMap<>();
    }
    return this.measurements;
  }

  public void setMeasurements(ConcurrentMap<String, Double> value) {
    this.measurements = value;
  }
}
