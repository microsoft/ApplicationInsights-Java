// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.json;

import com.azure.json.JsonSerializable;
import com.azure.json.JsonWriter;
import java.io.IOException;

public class LowerCaseEnumSerializers {

  private LowerCaseEnumSerializers() {}

  public static class LowerCaseEnumSerializer<T extends Enum<T>>
      implements JsonSerializable<LowerCaseEnumSerializer<T>> {
    private final T value;

    public LowerCaseEnumSerializer(T value) {
      this.value = value;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("value", value.name());
      jsonWriter.writeEndObject();
      return jsonWriter;
    }
  }

  public static class LowerCaseEnumDeSerializer<T extends Enum<T>>
      implements JsonSerializable<LowerCaseEnumDeSerializer<T>> {
    private Class<T> clazz;
    private T value;

    public LowerCaseEnumDeSerializer(Class<T> clazz) {
      this.clazz = clazz;
    }

    public Class<T> getClazz() {
      return clazz;
    }

    public LowerCaseEnumDeSerializer<T> setClazz(Class<T> clazz) {
      this.clazz = clazz;
      return this;
    }

    public T getValue() {
      return value;
    }

    public LowerCaseEnumDeSerializer<T> setValue(T value) {
      this.value = value;
      return this;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
      jsonWriter.writeStartObject();
      jsonWriter.writeStringField("clazz", clazz.getName());
      jsonWriter.writeStringField("value", value.name());
      jsonWriter.writeEndObject();
      return jsonWriter;
    }
  }
}
