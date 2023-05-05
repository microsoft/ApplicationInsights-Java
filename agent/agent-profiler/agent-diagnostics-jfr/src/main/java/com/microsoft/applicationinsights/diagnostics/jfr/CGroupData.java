// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.jfr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;

@SuppressWarnings({"Java8ApiChecker", "AbbreviationAsWordInName"})
@Name("com.microsoft.applicationinsights.diagnostics.jfr.CGroupData")
@Label("CGroupData")
@Category("Diagnostic")
@Description("CGroupData")
@StackTrace(false)
@Period("beginChunk")
public class CGroupData extends Event {

  public static final String NAME = "com.microsoft.applicationinsights.diagnostics.jfr.CGroupData";
  public static final int CGROUP_DATA_ABSENT = -2;

  // Limit of the kernel memory
  private final long kmemLimit; // /sys/fs/cgroup/memory/memory.kmem.limit_in_bytes

  // Limit of the containers memory
  private final long memoryLimit; // /sys/fs/cgroup/memory/memory.limit_in_bytes

  // Soft memory limit (enforced over the long term)
  private final long memorySoftLimit; // /sys/fs/cgroup/memory/memory.soft_limit_in_bytes

  // CPU usage limit
  private final long cpuLimit; // /sys/fs/cgroup/cpu,cpuacct/cpu.cfs_quota_us

  // CPU usage period
  private final long cpuPeriod; // /sys/fs/cgroup/cpu,cpuacct/cpu.cfs_quota_us

  @JsonCreator
  public CGroupData(
      @JsonProperty(value = "kmemLimit") long kmemLimit,
      @JsonProperty(value = "memoryLimit") long memoryLimit,
      @JsonProperty(value = "memorySoftLimit") long memorySoftLimit,
      @JsonProperty(value = "cpuLimit") long cpuLimit,
      @JsonProperty(value = "cpuPeriod", required = false) Long cpuPeriod) {
    this.kmemLimit = kmemLimit;
    this.memoryLimit = memoryLimit;
    this.memorySoftLimit = memorySoftLimit;
    this.cpuLimit = cpuLimit;

    if (cpuPeriod == null) {
      // No CGroup data was found for this value
      this.cpuPeriod = CGROUP_DATA_ABSENT;
    } else {
      this.cpuPeriod = cpuPeriod;
    }
  }

  public long getKmemLimit() {
    return kmemLimit;
  }

  public long getMemoryLimit() {
    return memoryLimit;
  }

  public long getMemorySoftLimit() {
    return memorySoftLimit;
  }

  public long getCpuLimit() {
    return cpuLimit;
  }

  public long getCpuPeriod() {
    return cpuPeriod;
  }
}
