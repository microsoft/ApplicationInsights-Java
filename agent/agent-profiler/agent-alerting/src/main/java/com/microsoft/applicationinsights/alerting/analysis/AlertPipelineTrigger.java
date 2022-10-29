// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.analysis;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * Observes a stream of data, and calls a downstream alert action if the below conditions are met.
 *
 * <ul>
 *   <li>data moves above given threshold
 *   <li>alert is not in a cooldown period
 *   <li>alert is enabled
 * </ul>
 */
public class AlertPipelineTrigger implements DoubleConsumer {
  private final AlertConfiguration alertConfig;
  private final Consumer<AlertBreach> action;
  private Instant lastAlertTime;

  public AlertPipelineTrigger(AlertConfiguration alertConfiguration, Consumer<AlertBreach> action) {
    this.alertConfig = alertConfiguration;
    this.action = action;
  }

  @Override
  public void accept(double telemetry) {
    if (alertConfig.isEnabled() && telemetry > alertConfig.getThreshold()) {
      if (isOffCooldown()) {
        lastAlertTime = Instant.now();
        UUID profileId = UUID.randomUUID();

        action.accept(
            AlertBreach.builder()
                .setType(alertConfig.getType())
                .setAlertValue(telemetry)
                .setAlertConfiguration(alertConfig)
                .setProfileId(profileId.toString())
                .build());
      }
    }
  }

  public boolean isOffCooldown() {
    Instant coolDownCutOff = Instant.now().minusSeconds(alertConfig.getCooldownSeconds());
    return lastAlertTime == null || lastAlertTime.isBefore(coolDownCutOff);
  }

  public Instant getLastAlertTime() {
    return lastAlertTime;
  }
}
