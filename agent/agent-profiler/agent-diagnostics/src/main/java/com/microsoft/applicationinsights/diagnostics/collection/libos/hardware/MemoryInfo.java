// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.hardware;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:MemberName"})
public class MemoryInfo {

  private final long totalInKB;

  private final long freeInKB;

  private final long virtualMemoryTotalInKB;

  private final long virtualMemoryUsedInKB;

  @JsonCreator
  public MemoryInfo(
      @JsonProperty("totalInKB") long totalInKB,
      @JsonProperty("freeInKB") long freeInKB,
      @JsonProperty("virtualMemoryTotalInKB") long virtualMemoryTotalInKB,
      @JsonProperty("virtualMemoryUsedInKB") long virtualMemoryUsedInKB) {

    this.totalInKB = totalInKB;
    this.freeInKB = freeInKB;
    this.virtualMemoryTotalInKB = virtualMemoryTotalInKB;
    this.virtualMemoryUsedInKB = virtualMemoryUsedInKB;
  }

  public long getTotalInKB() {
    return totalInKB;
  }

  public long getFreeInKB() {
    return freeInKB;
  }

  public long getVirtualMemoryTotalInKB() {
    return virtualMemoryTotalInKB;
  }

  public long getVirtualMemoryUsedInKB() {
    return virtualMemoryUsedInKB;
  }
}
