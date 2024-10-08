// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.azure.json.JsonSerializable;
import com.azure.json.JsonWriter;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

public class ProfilerConfiguration implements JsonSerializable<ProfilerConfiguration> {

  public static final Date DEFAULT_DATE;
  private Date lastModified;
  private boolean enabled;
  private String collectionPlan;
  private String cpuTriggerConfiguration;
  private String memoryTriggerConfiguration;
  private String defaultConfiguration;
  private List<AlertingConfig.RequestTrigger> requestTriggerConfiguration;

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

  public boolean hasBeenConfigured() {
    return getLastModified().compareTo(DEFAULT_DATE) != 0;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public ProfilerConfiguration setLastModified(Date lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public ProfilerConfiguration setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  @Nullable
  public String getCollectionPlan() {
    return collectionPlan;
  }

  public ProfilerConfiguration setCollectionPlan(String collectionPlan) {
    this.collectionPlan = collectionPlan;
    return this;
  }

  @Nullable
  public String getCpuTriggerConfiguration() {
    return cpuTriggerConfiguration;
  }

  public ProfilerConfiguration setCpuTriggerConfiguration(String cpuTriggerConfiguration) {
    this.cpuTriggerConfiguration = cpuTriggerConfiguration;
    return this;
  }

  @Nullable
  public String getMemoryTriggerConfiguration() {
    return memoryTriggerConfiguration;
  }

  public ProfilerConfiguration setMemoryTriggerConfiguration(String memoryTriggerConfiguration) {
    this.memoryTriggerConfiguration = memoryTriggerConfiguration;
    return this;
  }

  @Nullable
  public String getDefaultConfiguration() {
    return defaultConfiguration;
  }

  public ProfilerConfiguration setDefaultConfiguration(String defaultConfiguration) {
    this.defaultConfiguration = defaultConfiguration;
    return this;
  }

  @Nullable
  public List<AlertingConfig.RequestTrigger> getRequestTriggerConfiguration() {
    return requestTriggerConfiguration;
  }

  public ProfilerConfiguration setRequestTriggerConfiguration(
      List<AlertingConfig.RequestTrigger> requestTriggerConfiguration) {
    this.requestTriggerConfiguration = requestTriggerConfiguration;
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    jsonWriter.writeStartObject();
    jsonWriter.writeLongField("lastModified", lastModified.getTime());
    jsonWriter.writeBooleanField("enabled", enabled);
    jsonWriter.writeStringField("collectionPlan", collectionPlan);
    jsonWriter.writeStringField("cpuTriggerConfiguration", cpuTriggerConfiguration);
    jsonWriter.writeStringField("memoryTriggerConfiguration", memoryTriggerConfiguration);
    jsonWriter.writeStringField("defaultConfiguration", defaultConfiguration);
    jsonWriter.writeStartArray("requestTriggerConfiguration");
    for (AlertingConfig.RequestTrigger trigger : requestTriggerConfiguration) {
      trigger.toJson(jsonWriter);
    }
    jsonWriter.writeEndArray();
    jsonWriter.writeEndObject();
    return jsonWriter;
  }
}
