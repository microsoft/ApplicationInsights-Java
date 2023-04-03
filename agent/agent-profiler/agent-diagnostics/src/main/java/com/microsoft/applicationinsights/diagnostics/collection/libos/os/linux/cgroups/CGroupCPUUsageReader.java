// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CGroupCPUUsageReader extends CGroupValueReader {
  // total CPU usage (in nanoseconds) consumed by all tasks in this cgroup
  public CGroupCPUUsageReader() {
    super("/sys/fs/cgroup/cpu,cpuacct/cpuacct.usage");
  }
}
