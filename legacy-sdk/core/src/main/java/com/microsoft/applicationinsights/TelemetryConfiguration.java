package com.microsoft.applicationinsights;

// this class currently only exists so that 2.x bytecode instrumentation doesn't need to be changed
final class TelemetryConfiguration {

  boolean isTrackingDisabled() {
    return false;
  }
}
