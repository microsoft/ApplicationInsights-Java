// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.google.auto.value.AutoValue;
import java.time.Instant;

/** Contains the overall configuration of the entire alerting subsystem. */
@AutoValue
public abstract class AlertingConfiguration {

  public static AlertingConfiguration create(
      AlertConfiguration cpuAlert,
      AlertConfiguration memoryAlert,
      DefaultConfiguration defaultConfiguration,
      CollectionPlanConfiguration collectionPlanConfiguration) {
    return new AutoValue_AlertingConfiguration(
        cpuAlert, memoryAlert, defaultConfiguration, collectionPlanConfiguration);
  }

  public boolean hasAnEnabledTrigger() {
    boolean manualProfileEnabled =
        getCollectionPlanConfiguration().isSingle()
            && getCollectionPlanConfiguration().getMode()
                == CollectionPlanConfiguration.EngineMode.immediate
            && Instant.now().isBefore(getCollectionPlanConfiguration().getExpiration());

    return getCpuAlert().isEnabled() || manualProfileEnabled || getMemoryAlert().isEnabled();
    // Sampling not enabled yet
    // getDefaultConfiguration().getSamplingEnabled();
  }

  // Alert configuration for CPU telemetry
  public abstract AlertConfiguration getCpuAlert();

  // Alert configuration for MEMORY telemetry
  public abstract AlertConfiguration getMemoryAlert();

  // Alert configuration for the periodic profiling
  public abstract DefaultConfiguration getDefaultConfiguration();

  // Alert configuration for manual profiling
  public abstract CollectionPlanConfiguration getCollectionPlanConfiguration();
}
