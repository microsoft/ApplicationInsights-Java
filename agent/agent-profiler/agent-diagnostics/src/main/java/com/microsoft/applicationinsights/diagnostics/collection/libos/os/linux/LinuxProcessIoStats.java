// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import com.microsoft.applicationinsights.diagnostics.collection.libos.BigIncrementalCounter;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessIoStats;
import java.io.File;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Obtains per-process IO statistics. */
public class LinuxProcessIoStats extends TwoStepProcReader implements ProcessIoStats {
  private static final Logger logger = LoggerFactory.getLogger(LinuxProcessIoStats.class);

  private static final Pattern IO_READ_PATTERN =
      Pattern.compile("^rchar: (\\d+)$", Pattern.MULTILINE);
  private static final Pattern IO_WRITE_PATTERN =
      Pattern.compile("^wchar: (\\d+)$", Pattern.MULTILINE);
  private static final Pattern DISK_READ_PATTERN =
      Pattern.compile("^read_bytes: (\\d+)$", Pattern.MULTILINE);
  private static final Pattern DISK_WRITE_PATTERN =
      Pattern.compile("^write_bytes: (\\d+)$", Pattern.MULTILINE);

  protected final BigIncrementalCounter ioRead = new BigIncrementalCounter();
  protected final BigIncrementalCounter ioWrite = new BigIncrementalCounter();
  protected final BigIncrementalCounter diskRead = new BigIncrementalCounter();
  protected final BigIncrementalCounter diskWrite = new BigIncrementalCounter();

  private boolean canBeInspected = true;

  public LinuxProcessIoStats(File candidate) {
    super(new File(candidate, "io"), true);
    if (this.file == null) {
      // Generally indicates this process cannot be probed by this user
      this.canBeInspected = false;
    }
  }

  @Override
  public void poll() {
    if (canBeInspected) {
      super.poll();
    }
  }

  private static boolean findBigIntegerValue(
      Pattern pattern, String contents, BigIncrementalCounter counter) {
    Matcher matcher = pattern.matcher(contents);
    if (matcher.find()) {
      try {
        counter.newValue(new BigInteger(matcher.group(1)));
      } catch (NumberFormatException e) {
        logger.trace("Failed to parse {}", matcher.group(1));
      }
      return true;
    }
    return false;
  }

  @Override
  protected void parseLine(String line) {
    if (findBigIntegerValue(DISK_WRITE_PATTERN, line, diskWrite)) {
      return;
    }
    if (findBigIntegerValue(DISK_READ_PATTERN, line, diskRead)) {
      return;
    }
    if (findBigIntegerValue(IO_WRITE_PATTERN, line, ioWrite)) {
      return;
    }
    findBigIntegerValue(IO_READ_PATTERN, line, ioRead);
  }

  @Override
  public BigInteger getIoRead() {
    return ioRead.getIncrement();
  }

  @Override
  public BigInteger getIoWrite() {
    return ioWrite.getIncrement();
  }

  @Override
  public BigInteger getDiskRead() {
    return diskRead.getIncrement();
  }

  @Override
  public BigInteger getDiskWrite() {
    return diskWrite.getIncrement();
  }
}
