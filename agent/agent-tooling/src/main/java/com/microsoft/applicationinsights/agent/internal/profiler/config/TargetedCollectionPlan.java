// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;

public class TargetedCollectionPlan implements JsonSerializable<TargetedCollectionPlan> {

  @Nullable private List<String> roles;
  @Nullable private List<TargetedInstance> instances;
  private int immediateProfilingDuration;
  @Nullable private String expiration;
  @Nullable private String settingsMoniker;

  @Nullable
  public List<String> getRoles() {
    return roles;
  }

  public TargetedCollectionPlan setRoles(@Nullable List<String> roles) {
    this.roles = roles;
    return this;
  }

  @Nullable
  public List<TargetedInstance> getInstances() {
    return instances;
  }

  public TargetedCollectionPlan setInstances(@Nullable List<TargetedInstance> instances) {
    this.instances = instances;
    return this;
  }

  public int getImmediateProfilingDuration() {
    return immediateProfilingDuration;
  }

  public TargetedCollectionPlan setImmediateProfilingDuration(int immediateProfilingDuration) {
    this.immediateProfilingDuration = immediateProfilingDuration;
    return this;
  }

  @Nullable
  public String getExpiration() {
    return expiration;
  }

  public TargetedCollectionPlan setExpiration(@Nullable String expiration) {
    this.expiration = expiration;
    return this;
  }

  @Nullable
  public String getSettingsMoniker() {
    return settingsMoniker;
  }

  public TargetedCollectionPlan setSettingsMoniker(@Nullable String settingsMoniker) {
    this.settingsMoniker = settingsMoniker;
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    jsonWriter.writeStartObject();
    if (roles != null) {
      jsonWriter.writeArrayField("roles", roles, JsonWriter::writeString);
    }
    if (instances != null) {
      jsonWriter.writeArrayField("instances", instances, JsonWriter::writeJson);
    }
    jsonWriter.writeIntField("immediateProfilingDuration", immediateProfilingDuration);
    jsonWriter.writeStringField("expiration", expiration);
    jsonWriter.writeStringField("settingsMoniker", settingsMoniker);
    return jsonWriter.writeEndObject();
  }

  public static TargetedCollectionPlan fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        reader -> {
          TargetedCollectionPlan plan = new TargetedCollectionPlan();
          while (reader.nextToken() != JsonToken.END_OBJECT) {
            reader.nextToken();
            String fieldName = reader.getFieldName();
            if ("roles".equals(fieldName)) {
              plan.setRoles(reader.readArray(JsonReader::getString));
            } else if ("instances".equals(fieldName)) {
              plan.setInstances(reader.readArray(TargetedInstance::fromJson));
            } else if ("immediateProfilingDuration".equals(fieldName)) {
              plan.setImmediateProfilingDuration(reader.getInt());
            } else if ("expiration".equals(fieldName)) {
              plan.setExpiration(reader.getString());
            } else if ("settingsMoniker".equals(fieldName)) {
              plan.setSettingsMoniker(reader.getString());
            } else {
              reader.skipChildren();
            }
          }
          return plan;
        });
  }
}
