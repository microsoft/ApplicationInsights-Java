// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import com.microsoft.applicationinsights.diagnostics.collection.libos.hardware.MemoryInfo;
import com.microsoft.applicationinsights.diagnostics.collection.libos.hardware.MemoryInfoReader;
import java.io.File;

/** Reads memory usage info from /proc/meminfo */
public class LinuxMemoryInfoReader extends TwoStepProcReader implements MemoryInfoReader {
  private static final int MEMINFO_LINE_SUFFIX = " kB".length();
  private static final String MEMINFO = "/proc/meminfo";

  private int totalInKbIndex = -1;
  private int freeInKbIndex = -1;
  private int virtualMemoryTotalInKbIndex = -1;
  private int virtualMemoryUsedInKbIndex = -1;
  private MemoryInfo memoryInfo = new MemoryInfo(-1, -1, -1, -1);

  public LinuxMemoryInfoReader() {
    super(new File(MEMINFO));
  }

  public MemoryInfo readMemoryInfo(String content) {
    String[] lines = content.split("\n");

    if (totalInKbIndex == -1) {
      totalInKbIndex = LinuxKernelStats.lineIndexFor(lines, "MemTotal");
    }

    if (freeInKbIndex == -1) {
      freeInKbIndex = LinuxKernelStats.lineIndexFor(lines, "MemFree");
    }

    if (virtualMemoryTotalInKbIndex == -1) {
      virtualMemoryTotalInKbIndex = LinuxKernelStats.lineIndexFor(lines, "VmallocTotal");
    }

    if (virtualMemoryUsedInKbIndex == -1) {
      virtualMemoryUsedInKbIndex = LinuxKernelStats.lineIndexFor(lines, "VmallocUsed");
    }

    long totalInKb = readMemoryNumber(lines[totalInKbIndex]);
    long freeInKb = readMemoryNumber(lines[freeInKbIndex]);
    long virtualMemoryTotalInKb = readMemoryNumber(lines[virtualMemoryTotalInKbIndex]);
    long virtualMemoryUsedInKb = readMemoryNumber(lines[virtualMemoryUsedInKbIndex]);

    return new MemoryInfo(totalInKb, freeInKb, virtualMemoryTotalInKb, virtualMemoryUsedInKb);
  }

  private static long readMemoryNumber(String line) {
    int start = line.indexOf(':') + 1;
    int end = line.length() - MEMINFO_LINE_SUFFIX;
    String number = line.substring(start, end).trim();
    return Long.parseLong(number);
  }

  @Override
  public void update() {
    this.memoryInfo = readMemoryInfo(contents);
  }

  @Override
  protected void parseLine(String line) {}

  @Override
  public MemoryInfo getMemoryInfo() {
    return memoryInfo;
  }
}
