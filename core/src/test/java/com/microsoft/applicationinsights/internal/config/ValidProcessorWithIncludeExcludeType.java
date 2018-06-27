package com.microsoft.applicationinsights.internal.config;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.telemetry.Telemetry;

public class ValidProcessorWithIncludeExcludeType implements TelemetryProcessor {

  public void addToIncludedType(String item) {}

  public void addToExcludedType(String item) {}

  @Override
  public boolean process(Telemetry telemetry) {
    return false;
  }
}
