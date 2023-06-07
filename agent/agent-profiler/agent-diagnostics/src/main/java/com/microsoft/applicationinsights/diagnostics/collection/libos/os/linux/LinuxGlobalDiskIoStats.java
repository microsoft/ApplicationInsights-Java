// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import static com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.DiskNames.matchesDiskName;

import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.DiskStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.GlobalDiskStats;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts Disk IO stats (read/write volumes) from /proc */
public class LinuxGlobalDiskIoStats extends TwoStepProcReader implements GlobalDiskStats {

  private static final String DISKSTAT_FILE_LOCATION = "/proc/diskstats";

  private static final int READ_TIME = 4;
  private static final int WRITE_TIME = 8;
  private static final int IO_TIME = 10;
  public static final Pattern LETTERS = Pattern.compile("[A-Za-z]");

  private final Map<String, DiskStats> stats = new HashMap<>();

  private int indexOffset = -1;

  public static final String NAME_PATTERN = "^([^ ]+)";
  private static final Pattern NAME_MATCHER = Pattern.compile(NAME_PATTERN);

  public LinuxGlobalDiskIoStats() {
    super(DISKSTAT_FILE_LOCATION);
  }

  @Override
  protected void parseLine(String line) {
    int indexOffset = getOffset(line);

    line = line.substring(indexOffset);

    Matcher nameMatcher = NAME_MATCHER.matcher(line);
    if (nameMatcher.find()) {
      String interfaceName = nameMatcher.group(0);

      // Remove common lines
      if (interfaceName.startsWith("ram") || interfaceName.startsWith("loop")) {
        return;
      }

      if (matchesDiskName(interfaceName)) {
        String[] data = line.split(" ");
        long readTime = Long.parseLong(data[READ_TIME]);
        long writeTime = Long.parseLong(data[WRITE_TIME]);
        long ioTime = Long.parseLong(data[IO_TIME]);

        updateDisk(interfaceName, readTime, writeTime, ioTime);
      }
    }
  }

  private int getOffset(String line) {
    if (indexOffset == -1) {
      Matcher matcher = LETTERS.matcher(line);
      if (matcher.find()) {
        indexOffset = matcher.start();
      }
    }
    return indexOffset;
  }

  private void updateDisk(String diskName, long readTime, long writeTime, long ioTime) {
    DiskStats disk = stats.get(diskName);

    if (disk == null) {
      disk = new DiskStats(diskName);
      this.stats.put(diskName, disk);
    }
    disk.newReadTime(readTime);
    disk.newWriteTime(writeTime);
    disk.newIoTime(ioTime);
  }

  @Override
  public long getTotalWrite() {
    long accumulator = 0;
    for (DiskStats stat : stats.values()) {
      accumulator += stat.getWriteTime();
    }
    return accumulator;
  }

  @Override
  public long getTotalRead() {
    long accumulator = 0;
    for (DiskStats stat : stats.values()) {
      accumulator += stat.getReadTime();
    }
    return accumulator;
  }

  @Override
  public long getTotalIO() {
    long accumulator = 0;
    for (DiskStats stat : stats.values()) {
      accumulator += stat.getIoTime();
    }
    return accumulator;
  }

  @Override
  protected boolean trim() {
    return false;
  }
}
