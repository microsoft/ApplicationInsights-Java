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
  protected MessageData getData() {
    return data;
  }
}
