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

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.AvailabilityData;
import java.util.concurrent.ConcurrentMap;

public final class AvailabilityTelemetry extends BaseTelemetry {

  private final AvailabilityData data;

  public AvailabilityTelemetry() {
    data = new AvailabilityData();
    initialize(data.getProperties());
  }

  public String getId() {
    return data.getId();
  }

  public void setId(String id) {
    data.setId(id);
  }

  public String getName() {
    return data.getName();
  }

  public void setName(String name) {
    data.setName(name);
  }

  public Duration getDuration() {
    return data.getDuration();
  }

  public void setDuration(Duration duration) {
    data.setDuration(duration);
  }

  public boolean getSuccess() {
    return data.getSuccess();
  }

  public void setSuccess(boolean success) {
    data.setSuccess(success);
  }

  public String getRunLocation() {
    return data.getRunLocation();
  }

  public void setRunLocation(String runLocation) {
    data.setRunLocation(runLocation);
  }

  public String getMessage() {
    return data.getMessage();
  }

  public void setMessage(String message) {
    data.setMessage(message);
  }

  /** Gets a dictionary of custom defined metrics. */
  public ConcurrentMap<String, Double> getMetrics() {
    return data.getMeasurements();
  }

  @Override
  protected AvailabilityData getData() {
    return data;
  }
}
