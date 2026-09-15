// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;

/** Contains the overall configuration of the entire alerting subsystem. */
@AutoValue
public abstract class AlertingConfiguration {

  public static AlertingConfiguration create(
      AlertConfiguration cpuAlert,
      AlertConfiguration memoryAlert,
      DefaultConfiguration defaultConfiguration,
      CollectionPlanConfiguration collectionPlanConfiguration,
      List<AlertConfiguration> requestAlertConfiguration) {
    return create(
        cpuAlert,
        memoryAlert,
        defaultConfiguration,
        collectionPlanConfiguration,
        requestAlertConfiguration,
        null);
  }

  public static AlertingConfiguration create(
      AlertConfiguration cpuAlert,
      AlertConfiguration memoryAlert,
      DefaultConfiguration defaultConfiguration,
      CollectionPlanConfiguration collectionPlanConfiguration,
      List<AlertConfiguration> requestAlertConfiguration,
      @Nullable TargetedCollectionPlanConfiguration targetedCollectionPlanConfiguration) {
    return new AutoValue_AlertingConfiguration(
        cpuAlert,
        memoryAlert,
        defaultConfiguration,
        collectionPlanConfiguration,
        requestAlertConfiguration,
        targetedCollectionPlanConfiguration);
  }

  public boolean hasAnEnabledTrigger(
      @Nullable String roleName, @Nullable String roleInstance, Instant now) {
    CollectionPlanConfiguration collectionPlan = getCollectionPlanConfiguration();
    boolean manualProfileEnabled =
        collectionPlan.isSingle()
            && collectionPlan.getMode() == CollectionPlanConfiguration.EngineMode.immediate
            && now.isBefore(collectionPlan.getExpiration());

    TargetedCollectionPlanConfiguration targetedPlan = getTargetedCollectionPlanConfiguration();
    boolean onDemandProfileEnabled =
        targetedPlan == null
            ? manualProfileEnabled
            : targetedPlan.isActionable(roleName, roleInstance, now);

    return getCpuAlert().isEnabled() || onDemandProfileEnabled || getMemoryAlert().isEnabled();
  }

  public boolean hasRequestAlertConfiguration() {
    return getRequestAlertConfiguration() != null && !getRequestAlertConfiguration().isEmpty();
  }

  // Alert configuration for CPU telemetry
  public abstract AlertConfiguration getCpuAlert();

  // Alert configuration for MEMORY telemetry
  public abstract AlertConfiguration getMemoryAlert();

  // Alert configuration for the periodic profiling
  public abstract DefaultConfiguration getDefaultConfiguration();

  // Alert configuration for manual profiling
  public abstract CollectionPlanConfiguration getCollectionPlanConfiguration();

  // Alert configuration for SPAN telemetry
  public abstract List<AlertConfiguration> getRequestAlertConfiguration();

  @Nullable
  public abstract TargetedCollectionPlanConfiguration getTargetedCollectionPlanConfiguration();
}
