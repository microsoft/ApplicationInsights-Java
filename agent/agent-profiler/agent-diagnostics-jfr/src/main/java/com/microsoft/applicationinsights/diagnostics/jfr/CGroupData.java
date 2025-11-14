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

@SuppressWarnings({
  "Java8ApiChecker",
  "AbbreviationAsWordInName"
}) // JFR APIs require Java 11+, but agent targets Java 8 bytecode
@Name("com.microsoft.applicationinsights.diagnostics.jfr.CGroupData")
@Label("CGroupData")
@Category("Diagnostic")
@Description("CGroupData")
@StackTrace(false)
@Period("beginChunk")
public class CGroupData extends Event implements JsonSerializable<CGroupData> {

  public static final String NAME = "com.microsoft.applicationinsights.diagnostics.jfr.CGroupData";
  public static final int CGROUP_DATA_ABSENT = -2; // No CGroup data was found for this value

  // Limit of the kernel memory
  private long kmemLimit; // /sys/fs/cgroup/memory/memory.kmem.limit_in_bytes

  // Limit of the containers memory
  private long memoryLimit; // /sys/fs/cgroup/memory/memory.limit_in_bytes

  // Soft memory limit (enforced over the long term)
  private long memorySoftLimit; // /sys/fs/cgroup/memory/memory.soft_limit_in_bytes

  // CPU usage limit
  private long cpuLimit; // /sys/fs/cgroup/cpu,cpuacct/cpu.cfs_quota_us

  // CPU usage period
  private long cpuPeriod = CGROUP_DATA_ABSENT; // /sys/fs/cgroup/cpu,cpuacct/cpu.cfs_quota_us

  public long getKmemLimit() {
    return kmemLimit;
  }

  public CGroupData setKmemLimit(long kmemLimit) {
    this.kmemLimit = kmemLimit;
    return this;
  }

  public long getMemoryLimit() {
    return memoryLimit;
  }

  public CGroupData setMemoryLimit(long memoryLimit) {
    this.memoryLimit = memoryLimit;
    return this;
  }

  public long getMemorySoftLimit() {
    return memorySoftLimit;
  }

  public CGroupData setMemorySoftLimit(long memorySoftLimit) {
    this.memorySoftLimit = memorySoftLimit;
    return this;
  }

  public long getCpuLimit() {
    return cpuLimit;
  }

  public CGroupData setCpuLimit(long cpuLimit) {
    this.cpuLimit = cpuLimit;
    return this;
  }

  public long getCpuPeriod() {
    return cpuPeriod;
  }

  public CGroupData setCpuPeriod(long cpuPeriod) {
    this.cpuPeriod = cpuPeriod;
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    return jsonWriter
        .writeStartObject()
        .writeLongField("kmemLimit", kmemLimit)
        .writeLongField("memoryLimit", memoryLimit)
        .writeLongField("memorySoftLimit", memorySoftLimit)
        .writeLongField("cpuLimit", cpuLimit)
        .writeLongField("cpuPeriod", cpuPeriod)
        .writeEndObject();
  }

  public static CGroupData fromJson(JsonReader jsonReader) throws IOException {
    return jsonReader.readObject(
        reader -> {
          CGroupData deserializedValue = new CGroupData();

          while (reader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = reader.getFieldName();
            reader.nextToken();
            if ("kmemLimit".equals(fieldName)) {
              deserializedValue.setKmemLimit(reader.getLong());
            } else if ("memoryLimit".equals(fieldName)) {
              deserializedValue.setMemoryLimit(reader.getLong());
            } else if ("memorySoftLimit".equals(fieldName)) {
              deserializedValue.setMemorySoftLimit(reader.getLong());
            } else if ("cpuLimit".equals(fieldName)) {
              deserializedValue.setCpuLimit(reader.getLong());
            } else if ("cpuPeriod".equals(fieldName)) {
              deserializedValue.setCpuPeriod(reader.getLong());
            } else {
              reader.skipChildren();
            }
          }

          return deserializedValue;
        });
  }
}
