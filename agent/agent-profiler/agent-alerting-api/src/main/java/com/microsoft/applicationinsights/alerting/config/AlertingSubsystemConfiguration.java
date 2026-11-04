// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
public abstract class AlertingSubsystemConfiguration {

  public static AlertingSubsystemConfiguration create(
      @Nullable String roleName,
      @Nullable String roleInstance,
      AlertingProfileFileTriggerConfiguration profileFileTriggerConfiguration) {
    return new AutoValue_AlertingSubsystemConfiguration(
        roleName, roleInstance, profileFileTriggerConfiguration);
  }

  @Nullable
  public abstract String getRoleName();

  @Nullable
  public abstract String getRoleInstance();

  public abstract AlertingProfileFileTriggerConfiguration getProfileFileTriggerConfiguration();
}
