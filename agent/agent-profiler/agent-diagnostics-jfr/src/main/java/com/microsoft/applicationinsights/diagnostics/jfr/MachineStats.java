// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.jfr;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import java.io.IOException;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;

@SuppressWarnings("Java8ApiChecker") // JFR APIs require Java 11+, but agent targets Java 8 bytecode
@Name("com.microsoft.applicationinsights.diagnostics.jfr.MachineStats")
@Label("MachineStats")
@Category("Diagnostic")
@Description("MachineStats")
@StackTrace(false)
@Period("beginChunk")
public class MachineStats extends Event implements JsonSerializable<MachineStats> {
  public static final String NAME =
      "com.microsoft.applicationinsights.diagnostics.jfr.MachineStats";
  private double contextSwitchesPerMs;

  private int coreCount;

  public double getContextSwitchesPerMs() {
    return contextSwitchesPerMs;
  }

  public MachineStats setContextSwitchesPerMs(double contextSwitchesPerMs) {
    this.contextSwitchesPerMs = contextSwitchesPerMs;
    return this;
  }

  public int getCoreCount() {
    return coreCount;
  }

  public MachineStats setCoreCount(int coreCount) {
    this.coreCount = coreCount;
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    return jsonWriter
        .writeStartObject()
        .writeDoubleField("contextSwitchesPerMs", contextSwitchesPerMs)
        .writeIntField("coreCount", coreCount)
        .writeEndObject();
  }

  public static MachineStats fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        reader -> {
          MachineStats deserializedValue = new MachineStats();

          while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            if ("contextSwitchesPerMs".equals(fieldName)) {
              deserializedValue.setContextSwitchesPerMs(reader.getDouble());
            } else if ("coreCount".equals(fieldName)) {
              deserializedValue.setCoreCount(reader.getInt());
            } else {
              reader.skipChildren();
            }
          }

          return deserializedValue;
        });
  }
}
