// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupDataReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LinuxCGroupDataReader implements CGroupDataReader {

  private static final String CGROUP_DIR = "/sys/fs/cgroup";
  private static final String K_MEM_LIMIT_FILE = CGROUP_DIR + "/memory/memory.kmem.limit_in_bytes";
  private static final String MEM_LIMIT_FILE = CGROUP_DIR + "/memory/memory.limit_in_bytes";
  private static final String MEM_SOFT_LIMIT_FILE =
      CGROUP_DIR + "/memory/memory.soft_limit_in_bytes";
  private static final String CPU_LIMIT_FILE = CGROUP_DIR + "/cpu,cpuacct/cpu.cfs_quota_us";
  private static final String CPU_PERIOD_FILE = CGROUP_DIR + "/cpu,cpuacct/cpu.cfs_period_us";

  @Override
  public long getKmemLimit() throws OperatingSystemInteractionException {
    return readLong(K_MEM_LIMIT_FILE);
  }

  @Override
  public long getMemoryLimit() throws OperatingSystemInteractionException {
    return readLong(MEM_LIMIT_FILE);
  }

  @Override
  public long getMemorySoftLimit() throws OperatingSystemInteractionException {
    return readLong(MEM_SOFT_LIMIT_FILE);
  }

  @Override
  public long getCpuLimit() throws OperatingSystemInteractionException {
    return readLong(CPU_LIMIT_FILE);
  }

  @Override
  public long getCpuPeriod() throws OperatingSystemInteractionException {
    return readLong(CPU_PERIOD_FILE);
  }

  private static long readLong(String fileName) throws OperatingSystemInteractionException {
    try {
      File file = new File(fileName);
      if (!file.exists() || !file.isFile()) {
        throw new OperatingSystemInteractionException("File does not exist: " + fileName);
      }

      List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());
      if (lines.size() > 0) {
        return Long.parseLong(lines.get(0));
      } else {
        throw new OperatingSystemInteractionException("Unable to read value from: " + fileName);
      }
    } catch (Exception e) {
      throw new OperatingSystemInteractionException(e);
    }
  }
}
