package com.microsoft.applicationinsights.boot.initializer;

import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.context.DeviceContext;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Dhaval Doshi
 * This Telemetry Initializer is used to auto-configure cloud_RoleName field
 * to get a logical component on AppMap.
 */
public class SpringBootTelemetryInitializer implements TelemetryInitializer {

  @Value("${spring.application.name:application}")
  String appName;

  @Override
  public void initialize(Telemetry telemetry) {

    DeviceContext device = telemetry.getContext().getDevice();
    device.setRoleName(appName);
  }

}
