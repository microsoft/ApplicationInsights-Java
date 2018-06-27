package com.microsoft.applicationinsights.internal.config;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.telemetry.Telemetry;

public class TestProcessorThatThrowsOnIncludedAndExcludedTypes implements TelemetryProcessor {

  public void addToIncludedType(String item) throws Throwable {
    throw new Throwable();
  }

  public void addToExcludedType(String item) throws Throwable {
    throw new Throwable();
  }

  @Override
  public boolean process(Telemetry telemetry) {
    return false;
  }
}
