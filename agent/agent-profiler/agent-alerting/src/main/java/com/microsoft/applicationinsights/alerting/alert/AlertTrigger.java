// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.alert;

import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Observes a stream of data, and calls a downstream alert action if the below conditions are met.
 *
 * <ul>
 *   <li>data moves above given threshold
 *   <li>alert is not in a cooldown period
 *   <li>alert is enabled
 * </ul>
 */
public class AlertTrigger implements Consumer<Double> {

  private final AlertConfiguration alertConfig;
  private final Consumer<AlertBreach> action;
  private Instant lastAlertTime;

  public AlertTrigger(AlertConfiguration alertConfiguration, Consumer<AlertBreach> action) {
    this.alertConfig = alertConfiguration;
    this.action = action;
  }

  @Override
  public void accept(Double telemetry) {
    if (alertConfig.isEnabled() && telemetry > alertConfig.getThreshold()) {
      Instant coolDownCutOff = Instant.now().minusSeconds(alertConfig.getCooldown());
      if (lastAlertTime == null || lastAlertTime.isBefore(coolDownCutOff)) {
        lastAlertTime = Instant.now();
        UUID profileId = UUID.randomUUID();
        action.accept(
            new AlertBreach(alertConfig.getType(), telemetry, alertConfig, profileId.toString()));
      }
    }
  }
}
