// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.auto.value.AutoValue;
import java.text.ParseException;
import java.util.Date;
import javax.annotation.Nullable;

@AutoValue
public abstract class ProfilerConfiguration {

  public static final Date DEFAULT_DATE;

  static {
    Date defaultDate;
    try {
      defaultDate = new StdDateFormat().parse("0001-01-01T00:00:00+00:00");
    } catch (ParseException e) {
      // will not happen
      defaultDate = null;
    }
    DEFAULT_DATE = defaultDate;
  }

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
  }

  public boolean hasBeenConfigured() {
    return getLastModified().compareTo(DEFAULT_DATE) != 0;
  }

  public abstract Date getLastModified();

  public abstract boolean isEnabled();

  @Nullable
  public abstract String getCollectionPlan();

  public abstract String getCpuTriggerConfiguration();

  public abstract String getMemoryTriggerConfiguration();

  @Nullable
  public abstract String getDefaultConfiguration();
}
