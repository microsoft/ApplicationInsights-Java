// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting.config;

import com.google.auto.value.AutoValue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class TargetedCollectionPlanConfiguration {

  public static TargetedCollectionPlanConfiguration create(
      @Nullable List<String> roles,
      @Nullable List<TargetedInstanceConfiguration> instances,
      int immediateProfilingDurationSeconds,
      @Nullable Instant expiration,
      @Nullable String settingsMoniker) {
    return new AutoValue_TargetedCollectionPlanConfiguration(
        immutableCopy(roles),
        immutableCopy(instances),
        immediateProfilingDurationSeconds,
        expiration,
        settingsMoniker);
  }

  @Nullable
  public abstract List<String> getRoles();

  @Nullable
  public abstract List<TargetedInstanceConfiguration> getInstances();

  public abstract int getImmediateProfilingDurationSeconds();

  @Nullable
  public abstract Instant getExpiration();

  @Nullable
  public abstract String getSettingsMoniker();

  public boolean isValid() {
    List<String> roles = getRoles();
    List<TargetedInstanceConfiguration> instances = getInstances();
    if ((roles == null) == (instances == null)
        || getImmediateProfilingDurationSeconds() < 1
        || getImmediateProfilingDurationSeconds() > 360
        || getExpiration() == null
        || isBlank(getSettingsMoniker())) {
      return false;
    }

    if (roles != null) {
      if (roles.isEmpty()) {
        return false;
      }
      for (String role : roles) {
        if (isBlank(role)) {
          return false;
        }
      }
      return true;
    }

    if (instances.isEmpty()) {
      return false;
    }
    for (TargetedInstanceConfiguration instance : instances) {
      if (instance == null || isBlank(instance.getRole()) || isBlank(instance.getName())) {
        return false;
      }
    }
    return true;
  }

  public boolean isSelected(@Nullable String roleName, @Nullable String roleInstance) {
    if (!isValid() || isBlank(roleName)) {
      return false;
    }

    List<String> roles = getRoles();
    if (roles != null) {
      for (String role : roles) {
        if (equalsNormalized(role, roleName)) {
          return true;
        }
      }
      return false;
    }

    List<TargetedInstanceConfiguration> instances = getInstances();
    if (isBlank(roleInstance) || instances == null) {
      return false;
    }
    for (TargetedInstanceConfiguration instance : instances) {
      if (instance != null
          && equalsNormalized(instance.getRole(), roleName)
          && equalsNormalized(instance.getName(), roleInstance)) {
        return true;
      }
    }
    return false;
  }

  public boolean isActionable(
      @Nullable String roleName, @Nullable String roleInstance, Instant now) {
    Instant expiration = getExpiration();
    return expiration != null && now.isBefore(expiration) && isSelected(roleName, roleInstance);
  }

  @Nullable
  private static <T> List<T> immutableCopy(@Nullable List<T> values) {
    return values == null ? null : Collections.unmodifiableList(new ArrayList<>(values));
  }

  private static boolean equalsNormalized(@Nullable String left, @Nullable String right) {
    return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
  }

  private static boolean isBlank(@Nullable String value) {
    return value == null || value.trim().isEmpty();
  }
}
