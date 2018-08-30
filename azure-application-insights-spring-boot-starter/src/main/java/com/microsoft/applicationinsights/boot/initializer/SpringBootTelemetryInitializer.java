package com.microsoft.applicationinsights.boot.initializer;

import com.microsoft.applicationinsights.extensibility.TelemetryInitializer;
import com.microsoft.applicationinsights.extensibility.context.CloudContext;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.springframework.beans.factory.annotation.Value;

/**
 * <h1>TelemetryInitializer to set the CloudRoleName Instance</h1>
 *
 * <p>
 *  This Telemetry Initializer is used to auto-configure cloud_RoleName field
 *  to get a logical component on AppMap.
 * </p>
 *
 * @author Dhaval Doshi
 */
public class SpringBootTelemetryInitializer implements TelemetryInitializer {

  /** The Logical Name of SpringBoot Application*/
  @Value("${spring.application.name:application}")
  String appName;

  @Override
  public void initialize(Telemetry telemetry) {
    CloudContext device = telemetry.getContext().getCloud();
    device.setRole(appName);
  }

}
