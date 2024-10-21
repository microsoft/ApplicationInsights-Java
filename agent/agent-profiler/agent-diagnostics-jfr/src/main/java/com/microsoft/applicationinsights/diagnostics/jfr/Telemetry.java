// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.jfr;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;

@SuppressWarnings({"Java8ApiChecker"})
@Name("com.microsoft.applicationinsights.diagnostics.jfr.Telemetry")
@Label("Telemetry")
@Category("Diagnostic")
@Description("Telemetry")
@StackTrace(false)
@Period("100 ms")
public class Telemetry extends Event implements JsonSerializable<Telemetry> {
  public static final String NAME = "com.microsoft.applicationinsights.diagnostics.jfr.Telemetry";

  private static final int LATEST_VERSION = 3;

  private int version = 1;
  private String telemetry;

  public int getVersion() {
    return version;
  }

  public Telemetry setVersion(int version) {
    this.version = version;
    return this;
  }

  public String getTelemetry() {
    return telemetry;
  }

  @SuppressWarnings("unchecked")
  public Telemetry setTelemetry(Object telemetry) {
    if (telemetry instanceof List) {
      StringJoiner joiner = new StringJoiner(",");
      ((List<Double>) telemetry)
          .forEach(
              it -> {
                if (it == null) {
                  joiner.add("null");
                } else {
                  joiner.add(Double.toString(it));
                }
              });
      this.telemetry = joiner.toString();
      this.version = LATEST_VERSION;
    } else if (telemetry instanceof String) {
      this.telemetry = (String) telemetry;
      this.version = LATEST_VERSION;
    }
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    return jsonWriter
        .writeStartObject()
        .writeIntField("version", version)
        .writeStringField("telemetry", telemetry)
        .writeEndObject();
  }

  public static Telemetry fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        reader -> {
          Telemetry deserializedValue = new Telemetry();

          while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            // In this case field names are case-sensitive but this could be replaced with
            // 'equalsIgnoreCase' to
            // make them case-insensitive.
            if ("version".equals(fieldName)) {
              deserializedValue.setVersion(reader.getInt());
            } else if ("telemetry".equals(fieldName)) {
              deserializedValue.setTelemetry(reader.getString());
            } else {
              reader.skipChildren();
            }
          }

          return deserializedValue;
        });
  }
}
