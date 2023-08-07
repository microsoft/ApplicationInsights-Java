// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.hardware;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MemoryInfo {

  private final long totalInKb;

  private final long freeInKb;

  private final long virtualMemoryTotalInKb;

  private final long virtualMemoryUsedInKb;

  @JsonCreator
  public MemoryInfo(
      @JsonProperty("totalInKB") long totalInKb,
      @JsonProperty("freeInKB") long freeInKb,
      @JsonProperty("virtualMemoryTotalInKB") long virtualMemoryTotalInKb,
      @JsonProperty("virtualMemoryUsedInKB") long virtualMemoryUsedInKb) {

    this.totalInKb = totalInKb;
    this.freeInKb = freeInKb;
    this.virtualMemoryTotalInKb = virtualMemoryTotalInKb;
    this.virtualMemoryUsedInKb = virtualMemoryUsedInKb;
  }

  public long getTotalInKb() {
    return totalInKb;
  }

  public long getFreeInKb() {
    return freeInKb;
  }

  public long getVirtualMemoryTotalInKb() {
    return virtualMemoryTotalInKb;
  }

  public long getVirtualMemoryUsedInKb() {
    return virtualMemoryUsedInKb;
  }
}
