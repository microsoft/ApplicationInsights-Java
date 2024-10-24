// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.microsoft.applicationinsights.agent.internal.profiler.util.TimestampContract;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

public class ProfilerConfiguration implements JsonSerializable<ProfilerConfiguration> {

  public static final Date DEFAULT_DATE;
  // TODO find an alternative to com.fasterxml.jackson.databind.util.StdDateFormat
  private static final StdDateFormat STD_DATE_FORMAT;

  static {
    STD_DATE_FORMAT = new StdDateFormat();
    Date defaultDate;
    try {
      defaultDate = STD_DATE_FORMAT.parse("0001-01-01T00:00:00+00:00");
    } catch (ParseException e) {
      defaultDate = null;
    }
    DEFAULT_DATE = defaultDate;
  }

  private String id;
  private Date lastModified;
  private Date enabledLastModified;
  private boolean enabled;
  private String collectionPlan;
  private String cpuTriggerConfiguration;
  private String memoryTriggerConfiguration;
  private String defaultConfiguration;
  private List<AlertingConfig.RequestTrigger> requestTriggerConfiguration;

  public boolean hasBeenConfigured() {
    return getLastModified().compareTo(DEFAULT_DATE) != 0;
  }

  public String id() {
    return id;
  }

  public ProfilerConfiguration setId(String id) {
    this.id = id;
    return this;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public ProfilerConfiguration setLastModified(Date lastModified) {
    this.lastModified = lastModified;
    return this;
  }

  public Date getEnabledLastModified() {
    return enabledLastModified;
  }

  public ProfilerConfiguration setEnabledLastModified(Date enabledLastModified) {
    this.enabledLastModified = enabledLastModified;
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
    jsonWriter.writeStringField("id", id);
    jsonWriter.writeStringField("lastModified", STD_DATE_FORMAT.format(lastModified));
    jsonWriter.writeStringField(
        "enabledLastModified", TimestampContract.timestampToString(enabledLastModified));
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

  public static ProfilerConfiguration fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        reader -> {
          ProfilerConfiguration deserializedProfilerConfiguration = new ProfilerConfiguration();
          while (reader.nextToken() != JsonToken.END_OBJECT) {
            reader.nextToken();
            String fieldName = reader.getFieldName();
            if ("id".equals(fieldName)) {
              String id = reader.getString();
              deserializedProfilerConfiguration.setId(id);
            } else if ("lastModified".equals(fieldName)) {
              String lastModified = reader.getString();
              try {
                deserializedProfilerConfiguration.setLastModified(
                    new StdDateFormat().parse(lastModified));
              } catch (ParseException ignored) {
                deserializedProfilerConfiguration.setLastModified(DEFAULT_DATE);
              }
            } else if ("enabledLastModified".equals(fieldName)) {
              String enabledLastModified = reader.getString();
              try {
                deserializedProfilerConfiguration.setEnabledLastModified(
                    STD_DATE_FORMAT.parse(enabledLastModified));
              } catch (ParseException ignored) {
                deserializedProfilerConfiguration.setEnabledLastModified(DEFAULT_DATE);
              }
            } else if ("enabled".equals(fieldName)) {
              deserializedProfilerConfiguration.setEnabled(reader.getBoolean());
            } else if ("collectionPlan".equals(fieldName)) {
              deserializedProfilerConfiguration.setCollectionPlan(reader.getString());
            } else if ("cpuTriggerConfiguration".equals(fieldName)) {
              deserializedProfilerConfiguration.setCpuTriggerConfiguration(reader.getString());
            } else if ("memoryTriggerConfiguration".equals(fieldName)) {
              deserializedProfilerConfiguration.setMemoryTriggerConfiguration(reader.getString());
            } else if ("defaultConfiguration".equals(fieldName)) {
              deserializedProfilerConfiguration.setDefaultConfiguration(reader.getString());
            } else if ("requestTriggerConfiguration".equals(fieldName)) {
              deserializedProfilerConfiguration.setRequestTriggerConfiguration(
                  reader.readArray(AlertingConfig.RequestTrigger::fromJson));
            } else {
              reader.skipChildren();
            }
          }
          return deserializedProfilerConfiguration;
        });
  }
}
