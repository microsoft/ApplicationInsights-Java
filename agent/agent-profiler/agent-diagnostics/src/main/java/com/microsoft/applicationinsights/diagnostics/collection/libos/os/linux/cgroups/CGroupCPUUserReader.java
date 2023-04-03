// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CGroupCPUUserReader extends CGroupValueReader {
  // total user CPU time (in nanoseconds) consumed by all tasks in this cgroup
  public CGroupCPUUserReader() {
    super("/sys/fs/cgroup/cpu,cpuacct/cpuacct.usage_user");
  }
}
