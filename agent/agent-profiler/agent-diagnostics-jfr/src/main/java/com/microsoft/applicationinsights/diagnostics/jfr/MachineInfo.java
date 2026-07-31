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
@Name("com.microsoft.applicationinsights.diagnostics.jfr.MachineInfo")
@Label("MachineInfo")
@Category("Diagnostic")
@Description("MachineInfo")
@StackTrace(false)
@Period("beginChunk")
public class MachineInfo extends Event implements JsonSerializable<MachineInfo> {
  public static final String NAME = "com.microsoft.applicationinsights.diagnostics.jfr.MachineInfo";

  /**
   * Legacy calibration field. No longer populated (calibration was removed) but retained — along
   * with its accessor and deserialization — so previously-recorded recordings can still be read and
   * scored. New recordings emit it as 0.
   */
  private double contextSwitchesPerMs;

  private int coreCount;

  public double getContextSwitchesPerMs() {
    return contextSwitchesPerMs;
  }

  public MachineInfo setContextSwitchesPerMs(double contextSwitchesPerMs) {
    this.contextSwitchesPerMs = contextSwitchesPerMs;
    return this;
  }

  public int getCoreCount() {
    return coreCount;
  }

  public MachineInfo setCoreCount(int coreCount) {
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

  public static MachineInfo fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        reader -> {
          MachineInfo deserializedValue = new MachineInfo();

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
