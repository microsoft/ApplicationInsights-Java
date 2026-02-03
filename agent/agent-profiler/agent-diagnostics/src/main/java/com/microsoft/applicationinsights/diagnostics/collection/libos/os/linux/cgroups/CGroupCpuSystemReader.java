// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

import java.nio.file.Path;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CGroupCpuSystemReader extends CGroupValueReader {
  // total system CPU time (in nanoseconds) consumed by all tasks in this cgroup
  public CGroupCpuSystemReader(Path cgroupPath) {
    super(cgroupPath.resolve("./cpuacct.usage_sys"));
  }
}
