// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroupsv2;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupDataReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LinuxCGroupV2DataReader implements CGroupDataReader {
  private static final Path MEM_MAX_FILE = Paths.get("./memory.max");
  private static final Path MEM_HIGH_FILE = Paths.get("./memory.high");
  private static final Path CPU_MAX_FILE = Paths.get("./cpu.max");

  private final Path memMaxFile;
  private final Path memHighFile;
  private final Path cpuMaxFile;

  public LinuxCGroupV2DataReader(Path cgroupDir) {
    memMaxFile = cgroupDir.resolve(MEM_MAX_FILE);
    memHighFile = cgroupDir.resolve(MEM_HIGH_FILE);
    cpuMaxFile = cgroupDir.resolve(CPU_MAX_FILE);
  }

  @Override
  public long getKmemLimit() {
    // In cgroup v2, kernel memory accounting is not separately exposed
    // Return a large value to indicate no specific limit
    return Long.MAX_VALUE;
  }

  @Override
  public long getMemoryLimit() {
    return parseMemoryValue(memMaxFile);
  }

  @Override
  public long getMemorySoftLimit() {
    return parseMemoryValue(memHighFile);
  }

  @Override
  public long getCpuLimit() {
    return parseCpuQuota();
  }

  @Override
  public long getCpuPeriod() {
    return parseCpuPeriod();
  }

  @Override
  public boolean isAvailable() {
    return Files.exists(memMaxFile) || Files.exists(memHighFile) || Files.exists(cpuMaxFile);
  }

  private static long parseMemoryValue(Path file) {
    try {
      String content = readFileContent(file);
      if ("max".equalsIgnoreCase(content.trim())) {
        return Long.MAX_VALUE;
      }
      return Long.parseLong(content.trim());
    } catch (Exception e) {
      return Long.MAX_VALUE;
    }
  }

  private long parseCpuQuota() {
    try {
      String content = readFileContent(cpuMaxFile);
      String[] parts = content.trim().split("\\s+");
      if (parts.length >= 1) {
        if ("max".equalsIgnoreCase(parts[0])) {
          return -1; // No quota defined
        }
        return Long.parseLong(parts[0]);
      }
    } catch (Exception ignored) {
      return -1; // No quota defined
    }
    return -1; // No quota defined
  }

  private long parseCpuPeriod() {
    try {
      String content = readFileContent(cpuMaxFile);
      String[] parts = content.trim().split("\\s+");
      if (parts.length >= 2) {
        return Long.parseLong(parts[1]);
      }
    } catch (Exception ignored) {
      return -1; // No quota defined
    }
    return -1; // No period defined
  }

  private static String readFileContent(Path file) throws OperatingSystemInteractionException {
    try {
      if (!Files.exists(file) || !Files.isRegularFile(file)) {
        throw new OperatingSystemInteractionException(
            "File does not exist: " + file.getFileName().toString());
      }

      List<String> lines = Files.readAllLines(file, Charset.defaultCharset());
      if (lines.isEmpty()) {
        throw new OperatingSystemInteractionException(
            "Empty file: " + file.getFileName().toString());
      }
      return lines.get(0);
    } catch (Exception e) {
      throw new OperatingSystemInteractionException(e);
    }
  }
}
