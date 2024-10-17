// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.hardware;

import com.azure.json.JsonSerializable;
import com.azure.json.JsonWriter;
import java.io.IOException;

public class MemoryInfo implements JsonSerializable<MemoryInfo> {

  private long totalInKb;

  private long freeInKb;

  private long virtualMemoryTotalInKb;

  private long virtualMemoryUsedInKb;

  public long getTotalInKb() {
    return totalInKb;
  }

  public MemoryInfo setTotalInKb(long totalInKb) {
    this.totalInKb = totalInKb;
    return this;
  }

  public long getFreeInKb() {
    return freeInKb;
  }

  public MemoryInfo setFreeInKb(long freeInKb) {
    this.freeInKb = freeInKb;
    return this;
  }

  public long getVirtualMemoryTotalInKb() {
    return virtualMemoryTotalInKb;
  }

  public MemoryInfo setVirtualMemoryTotalInKb(long virtualMemoryTotalInKb) {
    this.virtualMemoryTotalInKb = virtualMemoryTotalInKb;
    return this;
  }

  public long getVirtualMemoryUsedInKb() {
    return virtualMemoryUsedInKb;
  }

  public MemoryInfo setVirtualMemoryUsedInKb(long virtualMemoryUsedInKb) {
    this.virtualMemoryUsedInKb = virtualMemoryUsedInKb;
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    jsonWriter.writeStartObject();
    jsonWriter.writeLongField("totalInKb", totalInKb);
    jsonWriter.writeLongField("freeInKb", freeInKb);
    jsonWriter.writeLongField("virtualMemoryTotalInKb", virtualMemoryTotalInKb);
    jsonWriter.writeLongField("virtualMemoryUsedInKb", virtualMemoryUsedInKb);
    jsonWriter.writeEndObject();
    return jsonWriter;
  }
}
