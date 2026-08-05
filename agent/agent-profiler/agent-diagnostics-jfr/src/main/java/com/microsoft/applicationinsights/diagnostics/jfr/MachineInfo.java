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

/**
 * Machine-level diagnostic event.
 *
 * <p><b>Compatibility contract.</b> The JFR wire event name ({@link #NAME}) is an explicitly
 * versioned identifier. It was renamed from {@link #LEGACY_NAME} ("MachineStats") to "MachineInfo".
 * Because the event name is the key by which a reader locates the event, changing it is a
 * compatibility boundary that {@link #SCHEMA_VERSION} (payload versioning) cannot bridge:
 *
 * <ul>
 *   <li><b>Read side (backward compatibility):</b> readers must look up {@link #NAME} and fall back
 *       to {@link #LEGACY_NAME} so that recordings produced by older agents (which carry a
 *       "MachineStats" event) can still be discovered and scored.
 *   <li><b>Write side (forward compatibility):</b> agents at this version and later emit only
 *       {@link #NAME}. Consumers/backends and any custom JFC files must therefore be updated to
 *       recognize "MachineInfo"; older consumers that only know "MachineStats" will not find the
 *       event. This is a deliberate, documented minimum-consumer-version requirement rather than a
 *       silent change.
 * </ul>
 *
 * <p>Once the write side no longer needs to interoperate with the old name, {@link #LEGACY_NAME}
 * and its read-side fallback can be removed.
 */
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
   * Event name emitted by agents prior to the MachineStats-&gt;MachineInfo rename. Readers fall
   * back to this so previously-recorded recordings (which carry a "MachineStats" event) can still
   * be located and scored.
   */
  public static final String LEGACY_NAME =
      "com.microsoft.applicationinsights.diagnostics.jfr.MachineStats";

  /**
   * Current schema version. Version 2 drops the legacy {@code contextSwitchesPerMs} field;
   * recordings produced before the schemaVersion field was added carry schemaVersion 1 (the
   * implicit legacy version) and still serialize {@code contextSwitchesPerMs}.
   */
  public static final int SCHEMA_VERSION = 2;

  private int coreCount;

  private int schemaVersion;

  public int getCoreCount() {
    return coreCount;
  }

  public MachineInfo setCoreCount(int coreCount) {
    this.coreCount = coreCount;
    return this;
  }

  public int getSchemaVersion() {
    return schemaVersion;
  }

  public MachineInfo setSchemaVersion(int schemaVersion) {
    this.schemaVersion = schemaVersion;
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    jsonWriter.writeStartObject();
    return jsonWriter
        .writeIntField("coreCount", coreCount)
        .writeIntField("schemaVersion", schemaVersion)
        .writeEndObject();
  }

  public static MachineInfo fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        reader -> {
          MachineInfo deserializedValue = new MachineInfo();

          while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            if ("coreCount".equals(fieldName)) {
              deserializedValue.setCoreCount(reader.getInt());
            } else if ("schemaVersion".equals(fieldName)) {
              deserializedValue.setSchemaVersion(reader.getInt());
            } else {
              reader.skipChildren();
            }
          }

          return deserializedValue;
        });
  }
}
