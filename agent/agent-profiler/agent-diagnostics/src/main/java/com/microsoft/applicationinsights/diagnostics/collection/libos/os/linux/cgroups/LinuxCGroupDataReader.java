// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupDataReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LinuxCGroupDataReader implements CGroupDataReader {

  private static final String K_MEM_LIMIT_FILE = "./memory/memory.kmem.limit_in_bytes";
  private static final String MEM_LIMIT_FILE = "./memory/memory.limit_in_bytes";
  private static final String MEM_SOFT_LIMIT_FILE = "./memory/memory.soft_limit_in_bytes";
  private static final String CPU_LIMIT_FILE = "./cpu,cpuacct/cpu.cfs_quota_us";
  private static final String CPU_PERIOD_FILE = "./cpu,cpuacct/cpu.cfs_period_us";

  private final Path kmemLimitFile;
  private final Path memLimitFile;
  private final Path memSoftLimitFile;
  private final Path cpuLimitFile;
  private final Path cpuPeriodFile;

  public LinuxCGroupDataReader(Path cgroupRoot) {
    kmemLimitFile = cgroupRoot.resolve(K_MEM_LIMIT_FILE);
    memLimitFile = cgroupRoot.resolve(MEM_LIMIT_FILE);
    memSoftLimitFile = cgroupRoot.resolve(MEM_SOFT_LIMIT_FILE);
    cpuLimitFile = cgroupRoot.resolve(CPU_LIMIT_FILE);
    cpuPeriodFile = cgroupRoot.resolve(CPU_PERIOD_FILE);
  }

  @Override
  public long getKmemLimit() throws OperatingSystemInteractionException {
    return readLong(kmemLimitFile);
  }

  @Override
  public long getMemoryLimit() throws OperatingSystemInteractionException {
    return readLong(memLimitFile);
  }

  @Override
  public long getMemorySoftLimit() throws OperatingSystemInteractionException {
    return readLong(memSoftLimitFile);
  }

  @Override
  public long getCpuLimit() throws OperatingSystemInteractionException {
    return readLong(cpuLimitFile);
  }

  @Override
  public long getCpuPeriod() throws OperatingSystemInteractionException {
    return readLong(cpuPeriodFile);
  }

  @Override
  public boolean isAvailable() {
    return Files.exists(cpuLimitFile)
        || Files.exists(memLimitFile)
        || Files.exists(memSoftLimitFile)
        || Files.exists(kmemLimitFile)
        || Files.exists(cpuPeriodFile);
  }

  private static long readLong(Path file) throws OperatingSystemInteractionException {
    try {
      if (!Files.exists(file) || !Files.isRegularFile(file)) {
        throw new OperatingSystemInteractionException("File does not exist: " + file.getFileName());
      }

      List<String> lines = Files.readAllLines(file, Charset.defaultCharset());
      if (!lines.isEmpty()) {
        return Long.parseLong(lines.get(0));
      } else {
        throw new OperatingSystemInteractionException(
            "Unable to read value from: " + file.getFileName());
      }
    } catch (Exception e) {
      throw new OperatingSystemInteractionException(e);
    }
  }
}
