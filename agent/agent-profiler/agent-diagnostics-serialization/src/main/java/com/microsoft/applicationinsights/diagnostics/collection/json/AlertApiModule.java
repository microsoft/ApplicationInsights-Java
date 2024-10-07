// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.json;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

public class AlertApiModule {

  public AlertApiModule() {
    addEnumConfig(AlertingConfig.RequestFilterType.class);
    addEnumConfig(AlertingConfig.RequestAggregationType.class);
    addEnumConfig(AlertingConfig.RequestTriggerThresholdType.class);
    addEnumConfig(AlertingConfig.RequestTriggerThrottlingType.class);
    addEnumConfig(AlertingConfig.RequestAggregationType.class);
  }

  private <T extends Enum<T>> void addEnumConfig(Class<T> clazz) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonWriter jsonWriter = JsonProviders.createWriter(outputStream);
        JsonReader jsonReader = JsonProviders.createReader(outputStream.toByteArray())) {
      addSerializer(jsonWriter, clazz);
      addDeserializer(jsonReader, clazz);
    } catch (IOException ignored) {
      // Ignored
    }
  }

  private static <T extends Enum<T>> void addSerializer(JsonWriter jsonWriter, Class<T> clazz)
      throws IOException {
    jsonWriter.writeStartObject(clazz.getSimpleName());
    for (T enumConstant : clazz.getEnumConstants()) {
      jsonWriter.writeStringField(
          enumConstant.name().toLowerCase(Locale.ROOT).replace("_", "-"), enumConstant.name());
    }
    jsonWriter.writeEndObject();
  }

  private static <T extends Enum<T>> T addDeserializer(JsonReader jsonReader, Class<T> clazz)
      throws IOException {
    return jsonReader.readObject(
        reader -> {
          if (reader.nextToken() != JsonToken.END_OBJECT) {
            return Enum.valueOf(
                clazz, jsonReader.getString().toUpperCase(Locale.ROOT).replace("-", "_"));
          } else {
            reader.skipChildren();
            return null;
          }
        });
  }
}
