// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
public abstract class TargetedInstanceConfiguration {

  public static TargetedInstanceConfiguration create(@Nullable String role, @Nullable String name) {
    return new AutoValue_TargetedInstanceConfiguration(role, name);
  }

  @Nullable
  public abstract String getRole();

  @Nullable
  public abstract String getName();
}
