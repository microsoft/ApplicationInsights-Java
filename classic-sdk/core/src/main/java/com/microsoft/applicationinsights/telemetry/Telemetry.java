// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.telemetry;

import java.util.Date;
import java.util.Map;

/** The base telemetry type interface for application insights. */
public interface Telemetry {

  /** Gets the time when telemetry was recorded */
  Date getTimestamp();

  /** Sets the time when telemetry was recorded */
  void setTimestamp(Date date);

  /** Gets the context associated with this telemetry instance. */
  TelemetryContext getContext();

  /** Gets the map of application-defined property names and values. */
  Map<String, String> getProperties();
}
