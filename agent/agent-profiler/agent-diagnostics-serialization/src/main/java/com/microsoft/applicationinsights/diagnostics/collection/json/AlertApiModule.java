// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.json;

import com.azure.json.JsonSerializable;
import com.azure.json.JsonWriter;
import com.microsoft.applicationinsights.alerting.aiconfig.AlertingConfig;
import java.io.IOException;

public class AlertApiModule implements JsonSerializable<AlertApiModule> {

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    jsonWriter.writeStartObject();
    addEnumConfig(jsonWriter, AlertingConfig.RequestFilterType.class);
    addEnumConfig(jsonWriter, AlertingConfig.RequestAggregationType.class);
    addEnumConfig(jsonWriter, AlertingConfig.RequestTriggerThresholdType.class);
    addEnumConfig(jsonWriter, AlertingConfig.RequestTriggerThrottlingType.class);
    addEnumConfig(jsonWriter, AlertingConfig.RequestAggregationType.class);
    jsonWriter.writeEndObject();
    return jsonWriter;
  }

  private <T extends Enum<T>> void addEnumConfig(JsonWriter jsonWriter, Class<T> clazz)
      throws IOException {
    jsonWriter.writeStartObject(clazz.getSimpleName());
    for (T enumConstant : clazz.getEnumConstants()) {
      jsonWriter.writeStringField(
          enumConstant.name().toLowerCase().replace("_", "-"), enumConstant.name());
    }
    jsonWriter.writeEndObject();
  }
}
