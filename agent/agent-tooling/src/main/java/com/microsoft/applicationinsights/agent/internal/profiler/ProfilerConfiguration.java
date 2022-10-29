// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.squareup.moshi.Json;
import java.util.Date;

public class ProfilerConfiguration {

  @Json(name = "lastModified")
  private final Date lastModified;

  @Json(name = "enabled")
  private final boolean enabled;

  @Json(name = "cpuTriggerConfiguration")
  private final String cpuTriggerConfiguration;

  @Json(name = "memoryTriggerConfiguration")
  private final String memoryTriggerConfiguration;

  @Json(name = "collectionPlan")
  private final String collectionPlan;

  @Json(name = "defaultConfiguration")
  private final String defaultConfiguration;

  public ProfilerConfiguration(
      Date lastModified,
      boolean enabled,
      String collectionPlan,
      String cpuTriggerConfiguration,
      String memoryTriggerConfiguration,
      String defaultConfiguration) {
    this.lastModified = new Date(lastModified.getTime());
    this.enabled = enabled;
    this.collectionPlan = collectionPlan;
    this.cpuTriggerConfiguration = cpuTriggerConfiguration;
    this.memoryTriggerConfiguration = memoryTriggerConfiguration;
    this.defaultConfiguration = defaultConfiguration;
  }

  public Date getLastModified() {
    return new Date(lastModified.getTime());
  }

  public String getCollectionPlan() {
    return collectionPlan;
  }

  public String getCpuTriggerConfiguration() {
    return cpuTriggerConfiguration;
  }

  public String getMemoryTriggerConfiguration() {
    return memoryTriggerConfiguration;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getDefaultConfiguration() {
    return defaultConfiguration;
  }
}
