// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroupsv2;

import com.microsoft.applicationinsights.diagnostics.collection.libos.BigIncrementalCounter;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.TwoStepProcReader;
import java.nio.file.Path;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CGroupv2CpuReader extends TwoStepProcReader {

  private static final String CPU_USAGE_PROPERTY = "usage_usec";
  private static final String CPU_SYSTEM_PROPERTY = "system_usec";
  private static final String CPU_USER_PROPERTY = "user_usec";

  private final BigIncrementalCounter cpuUsage = new BigIncrementalCounter();
  private final BigIncrementalCounter cpuSystem = new BigIncrementalCounter();
  private final BigIncrementalCounter cpuUser = new BigIncrementalCounter();

  // total CPU usage (in microseconds) consumed by all tasks in this cgroup
  public CGroupv2CpuReader(Path cgroupDir) {
    super(cgroupDir.resolve("./cpu.stat").toFile());
  }

  @Override
  protected void parseLine(String line) {
    String[] tokens = line.split(" ");

    if (tokens.length == 2) {
      if (CPU_USAGE_PROPERTY.equals(tokens[0])) {
        cpuUsage.newValue(Long.parseLong(tokens[1]));
      } else if (CPU_SYSTEM_PROPERTY.equals(tokens[0])) {
        cpuSystem.newValue(Long.parseLong(tokens[1]));
      } else if (CPU_USER_PROPERTY.equals(tokens[0])) {
        cpuUser.newValue(Long.parseLong(tokens[1]));
      }
    }
  }

  public BigIncrementalCounter getCpuUsage() {
    return cpuUsage;
  }

  public BigIncrementalCounter getCpuSystem() {
    return cpuSystem;
  }

  public BigIncrementalCounter getCpuUser() {
    return cpuUser;
  }
}
