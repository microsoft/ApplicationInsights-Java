// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;
import javax.annotation.Nullable;

public class TargetedInstance implements JsonSerializable<TargetedInstance> {

  @Nullable private String role;
  @Nullable private String name;

  @Nullable
  public String getRole() {
    return role;
  }

  public TargetedInstance setRole(@Nullable String role) {
    this.role = role;
    return this;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public TargetedInstance setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    return jsonWriter
        .writeStartObject()
        .writeStringField("role", role)
        .writeStringField("name", name)
        .writeEndObject();
  }

  public static TargetedInstance fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        reader -> {
          TargetedInstance instance = new TargetedInstance();
          while (reader.nextToken() != JsonToken.END_OBJECT) {
            reader.nextToken();
            String fieldName = reader.getFieldName();
            if ("role".equals(fieldName)) {
              instance.setRole(reader.getString());
            } else if ("name".equals(fieldName)) {
              instance.setName(reader.getString());
            } else {
              reader.skipChildren();
            }
          }
          return instance;
        });
  }
}
