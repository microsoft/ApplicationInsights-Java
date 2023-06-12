// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CGroupCPUSystemReader extends CGroupValueReader {
  // total system CPU time (in nanoseconds) consumed by all tasks in this cgroup
  public CGroupCPUSystemReader() {
    super("/sys/fs/cgroup/cpu,cpuacct/cpuacct.usage_sys");
  }
}
