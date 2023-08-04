// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

@SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "MemberName"})
public class CGroupCpuSystemReader extends CGroupValueReader {
  // total system CPU time (in nanoseconds) consumed by all tasks in this cgroup
  public CGroupCpuSystemReader() {
    super("/sys/fs/cgroup/cpu,cpuacct/cpuacct.usage_sys");
  }
}
