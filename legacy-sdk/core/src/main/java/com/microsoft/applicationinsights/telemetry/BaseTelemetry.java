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

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/** Superclass for all telemetry data classes. */
public abstract class BaseTelemetry implements Telemetry {

  private TelemetryContext context;
  private Date timestamp;

  protected BaseTelemetry() {}

  /** Initializes the instance with the context properties */
  protected void initialize(ConcurrentMap<String, String> properties) {
    context = new TelemetryContext(properties, new ContextTagsMap());
  }

  /** Gets date and time when event was recorded. */
  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  /** Sets date and time when event was recorded. */
  @Override
  public void setTimestamp(Date date) {
    timestamp = date;
  }

  /** Gets the context associated with the current telemetry item. */
  @Override
  public TelemetryContext getContext() {
    return context;
  }

  /**
   * Gets a dictionary of application-defined property names and values providing additional
   * information about this event.
   */
  @Override
  public Map<String, String> getProperties() {
    return context.getProperties();
  }

  protected abstract Object getData();
}
