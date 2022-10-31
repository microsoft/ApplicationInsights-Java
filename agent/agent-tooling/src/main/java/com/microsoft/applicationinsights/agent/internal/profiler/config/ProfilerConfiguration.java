// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.Date;
import javax.annotation.Nullable;

@AutoValue
public abstract class ProfilerConfiguration {

  @JsonCreator
  public static ProfilerConfiguration create(
      @JsonProperty("lastModified") Date lastModified,
      @JsonProperty("enabled") boolean enabled,
      @JsonProperty("collectionPlan") String collectionPlan,
      @JsonProperty("cpuTriggerConfiguration") String cpuTriggerConfiguration,
      @JsonProperty("memoryTriggerConfiguration") String memoryTriggerConfiguration,
      @JsonProperty("defaultConfiguration") String defaultConfiguration) {

    return new AutoValue_ProfilerConfiguration(
        lastModified,
        enabled,
        collectionPlan,
        cpuTriggerConfiguration,
        memoryTriggerConfiguration,
        defaultConfiguration);
    //    this.lastModified = new Date(lastModified.getTime());
    //    this.enabled = enabled;
    //    this.collectionPlan = collectionPlan;
    //    this.cpuTriggerConfiguration = cpuTriggerConfiguration;
    //    this.memoryTriggerConfiguration = memoryTriggerConfiguration;
    //    this.defaultConfiguration = defaultConfiguration;
  }

  public abstract Date getLastModified();

  public abstract boolean isEnabled();

  public abstract String getCollectionPlan();

  public abstract String getCpuTriggerConfiguration();

  public abstract String getMemoryTriggerConfiguration();

  @Nullable
  public abstract String getDefaultConfiguration();
}
