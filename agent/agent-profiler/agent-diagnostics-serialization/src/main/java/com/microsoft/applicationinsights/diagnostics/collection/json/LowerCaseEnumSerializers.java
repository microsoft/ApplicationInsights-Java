// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Locale;

public class LowerCaseEnumSerializers {

  private LowerCaseEnumSerializers() {}

  public static class LowerCaseEnumSerializer<T extends Enum<T>> extends JsonSerializer<T> {
    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.name().toLowerCase(Locale.ROOT).replace("_", "-"));
    }
  }

  public static class LowerCaseEnumDeSerializer<T extends Enum<T>> extends JsonDeserializer<T> {

    private final Class<T> clazz;

    public LowerCaseEnumDeSerializer(Class<T> clazz) {
      this.clazz = clazz;
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return Enum.valueOf(
          clazz, p.getValueAsString().toUpperCase(Locale.ROOT).replaceAll("-", "_"));
    }
  }
}
