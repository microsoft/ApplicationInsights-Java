// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.StatsbeatTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.configuration.StatsbeatConnectionString;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;

abstract class BaseStatsbeat {

  private final CustomDimensions customDimensions;

  protected BaseStatsbeat(CustomDimensions customDimensions) {
    this.customDimensions = customDimensions;
  }

  protected abstract void send(TelemetryClient telemetryClient);

  protected StatsbeatTelemetryBuilder createStatsbeatTelemetry(
      TelemetryClient telemetryClient, String name, double value) {

    StatsbeatTelemetryBuilder telemetryBuilder = StatsbeatTelemetryBuilder.create(name, value);

    StatsbeatConnectionString connectionString = telemetryClient.getStatsbeatConnectionString();
    if (connectionString != null) {
      // not sure if connectionString can be null in Azure Functions
      telemetryBuilder.setConnectionString(connectionString);
    }

    customDimensions.populateProperties(telemetryBuilder, telemetryClient.getInstrumentationKey());

    return telemetryBuilder;
  }
}
