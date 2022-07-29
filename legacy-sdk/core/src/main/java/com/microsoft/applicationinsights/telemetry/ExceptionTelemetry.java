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

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;

/** Telemetry type used to track exceptions sent to Azure Application Insights. */
public final class ExceptionTelemetry extends BaseTelemetry {

  private final ExceptionData data;
  private Throwable throwable;

  public ExceptionTelemetry() {
    data = new ExceptionData();
    initialize(data.getProperties());
  }

  /**
   * Creates a new instance.
   *
   * @param stackSize The max stack size to report.
   * @param throwable The exception to track.
   */
  public ExceptionTelemetry(Throwable throwable, int stackSize) {
    this();
    this.throwable = throwable;
  }

  /**
   * Creates a new instance.
   *
   * @param throwable The exception to track.
   */
  public ExceptionTelemetry(Throwable throwable) {
    this(throwable, Integer.MAX_VALUE);
  }

  public Throwable getThrowable() {
    return throwable;
  }

  public void setException(Throwable throwable) {
    setException(throwable, Integer.MAX_VALUE);
  }

  public void setException(Throwable throwable, int stackSize) {
    this.throwable = throwable;
  }

  /**
   * Gets a map of application-defined exception metrics. The metrics appear along with the
   * exception in Analytics, but under Custom Metrics in Metrics Explorer.
   */
  public ConcurrentMap<String, Double> getMetrics() {
    return data.getMeasurements();
  }

  public void setSeverityLevel(SeverityLevel severityLevel) {
    data.setSeverityLevel(
        severityLevel == null
            ? null
            : com.microsoft.applicationinsights.internal.schemav2.SeverityLevel.values()[
                severityLevel.getValue()]);
  }

  @Nullable
  public SeverityLevel getSeverityLevel() {
    return data.getSeverityLevel() == null
        ? null
        : SeverityLevel.values()[data.getSeverityLevel().getValue()];
  }

  @Override
  protected ExceptionData getData() {
    return data;
  }
}
