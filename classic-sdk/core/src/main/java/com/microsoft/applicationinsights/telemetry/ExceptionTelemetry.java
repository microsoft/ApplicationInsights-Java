// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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

  // this is required for interop with versions of the Java agent prior to 3.4.0
  @Nullable
  public Exception getException() {
    return throwable instanceof Exception ? (Exception) throwable : null;
  }

  public void setException(Throwable throwable) {
    setException(throwable, Integer.MAX_VALUE);
  }

  public void setException(Throwable throwable, int stackSize) {
    this.throwable = throwable;
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

  /** Gets a dictionary of custom defined metrics. */
  public ConcurrentMap<String, Double> getMetrics() {
    return data.getMeasurements();
  }

  @Override
  protected ExceptionData getData() {
    return data;
  }
}
