// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import javax.annotation.Nullable;

/** Telemetry type used for log messages. */
public final class TraceTelemetry extends BaseTelemetry {

  private final MessageData data;

  public TraceTelemetry() {
    this("");
  }

  public TraceTelemetry(String message) {
    this(message, null);
  }

  /**
   * Creates a new instance.
   *
   * @param message The message. Max length 10000.
   * @param severityLevel The severity level.
   */
  public TraceTelemetry(String message, @Nullable SeverityLevel severityLevel) {
    data = new MessageData();
    initialize(data.getProperties());

    setMessage(message);
    setSeverityLevel(severityLevel);
  }

  /**
   * Gets the message text. For example, the text that would normally be written to a log file line.
   */
  public String getMessage() {
    return data.getMessage();
  }

  /**
   * Sets the message text. For example, the text that would normally be written to a log file line.
   */
  public void setMessage(String message) {
    data.setMessage(message);
  }

  public void setSeverityLevel(SeverityLevel severityLevel) {
    data.setSeverityLevel(
        severityLevel == null
            ? null
            : mapToInternalSeverityLevel(severityLevel));
  }

  @Nullable
  public SeverityLevel getSeverityLevel() {
    return data.getSeverityLevel() == null
        ? null
        : mapFromInternalSeverityLevel(data.getSeverityLevel());
  }

  @Override
  protected MessageData getData() {
    return data;
  }

  private static com.microsoft.applicationinsights.internal.schemav2.SeverityLevel mapToInternalSeverityLevel(SeverityLevel severityLevel) {
    switch (severityLevel) {
      case Verbose:
        return com.microsoft.applicationinsights.internal.schemav2.SeverityLevel.Verbose;
      case Information:
        return com.microsoft.applicationinsights.internal.schemav2.SeverityLevel.Information;
      case Warning:
        return com.microsoft.applicationinsights.internal.schemav2.SeverityLevel.Warning;
      case Error:
        return com.microsoft.applicationinsights.internal.schemav2.SeverityLevel.Error;
      case Critical:
        return com.microsoft.applicationinsights.internal.schemav2.SeverityLevel.Critical;
      default:
        throw new IllegalArgumentException("Unknown SeverityLevel: " + severityLevel);
    }
  }

  private static SeverityLevel mapFromInternalSeverityLevel(com.microsoft.applicationinsights.internal.schemav2.SeverityLevel internalSeverityLevel) {
    switch (internalSeverityLevel) {
      case Verbose:
        return SeverityLevel.Verbose;
      case Information:
        return SeverityLevel.Information;
      case Warning:
        return SeverityLevel.Warning;
      case Error:
        return SeverityLevel.Error;
      case Critical:
        return SeverityLevel.Critical;
      default:
        throw new IllegalArgumentException("Unknown internal SeverityLevel: " + internalSeverityLevel);
    }
  }
}
