// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import com.microsoft.applicationinsights.diagnostics.collection.libos.BigIncrementalCounter;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessCpuStats;
import java.io.File;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Gathers CPU usage statistics for a given process */
public class LinuxProcessCpuStats extends TwoStepProcReader implements ProcessCpuStats {
  private static final int USER_TIME_OFFSET_FROM_NAME = 12;
  private static final int SYSTEM_TIME_OFFSET_FROM_NAME = 13;
  private static final int PRIORITY_OFFSET_FROM_NAME = 16;
  private static final int NICE_TIME_OFFSET_FROM_NAME = 17;
  private static final int NUM_THREADS_OFFSET_FROM_NAME = 18;
  private static final int VM_SIZE_OFFSET_FROM_NAME = 21;
  private static final int RSS_OFFSET_FROM_NAME = 22;
  private static final int N_SWAPPED_OFFSET_FROM_NAME = 34;
  private static final Logger logger = LoggerFactory.getLogger(LinuxProcessCpuStats.class);

  private final BigIncrementalCounter userTime;
  private final BigIncrementalCounter systemTime;
  private final BigIncrementalCounter priority;
  private final BigIncrementalCounter nice;
  private final BigIncrementalCounter numThreads;
  private final BigIncrementalCounter vmSize;
  private final BigIncrementalCounter rss;
  private final BigIncrementalCounter swapped;

  public LinuxProcessCpuStats(File candidate) {
    super(new File(candidate, "stat"));
    this.userTime = new BigIncrementalCounter();
    this.systemTime = new BigIncrementalCounter();
    this.priority = new BigIncrementalCounter();
    this.nice = new BigIncrementalCounter();
    this.numThreads = new BigIncrementalCounter();
    this.vmSize = new BigIncrementalCounter();
    this.rss = new BigIncrementalCounter();
    this.swapped = new BigIncrementalCounter();
  }

  @Override
  public void update() {
    updateProcessTimes(contents, contents.indexOf(')'));
  }

  private void updateProcessTimes(String contents, int nameEnd) {
    String userTime = getVaule(contents, nameEnd, USER_TIME_OFFSET_FROM_NAME);
    String systemTime = getVaule(contents, nameEnd, SYSTEM_TIME_OFFSET_FROM_NAME);
    String priority = getVaule(contents, nameEnd, PRIORITY_OFFSET_FROM_NAME);
    String nice = getVaule(contents, nameEnd, NICE_TIME_OFFSET_FROM_NAME);
    String numThreads = getVaule(contents, nameEnd, NUM_THREADS_OFFSET_FROM_NAME);
    String vmSize = getVaule(contents, nameEnd, VM_SIZE_OFFSET_FROM_NAME);
    String rss = getVaule(contents, nameEnd, RSS_OFFSET_FROM_NAME);
    String swapped = getVaule(contents, nameEnd, N_SWAPPED_OFFSET_FROM_NAME);

    setValue(this.userTime, userTime);
    setValue(this.systemTime, systemTime);
    setValue(this.priority, priority);
    setValue(this.nice, nice);
    setValue(this.numThreads, numThreads);
    setValue(this.vmSize, vmSize);
    setValue(this.rss, rss);
    setValue(this.swapped, swapped);
  }

  private static void setValue(BigIncrementalCounter userTime, String userTime1) {
    try {
      userTime.newValue(new BigInteger(userTime1));
    } catch (NumberFormatException e) {
      logger.trace("Failed to parse {}", userTime1);
    }
  }

  private static String getVaule(String contents, int nameEnd, int index) {
    int start = spaceOffset(contents, nameEnd, index) + 1;
    int end = contents.indexOf(' ', start + 1);
    return contents.substring(start, end);
  }

  private static int spaceOffset(String s, int fromIndex, int numberOfSpaces) {
    int index;
    for (index = fromIndex; numberOfSpaces > 0; numberOfSpaces--) {
      index = s.indexOf(' ', index + 1);
    }
    return index;
  }

  @Override
  protected void parseLine(String line) {}

  @Override
  public BigInteger getUserTime() {
    return userTime.getIncrement();
  }

  @Override
  public BigInteger getSystemTime() {
    return systemTime.getIncrement();
  }

  @Override
  public BigInteger getTotalTime() {
    return getUserTime().add(getSystemTime());
  }

  @Override
  public BigInteger getPriority() {
    return priority.getValue();
  }

  @Override
  public BigInteger getNice() {
    return nice.getValue();
  }

  @Override
  public BigInteger getNumThreads() {
    return this.numThreads.getValue();
  }

  @Override
  public BigInteger getVmSize() {
    return vmSize.getValue();
  }

  @Override
  public BigInteger getRss() {
    return rss.getValue();
  }

  @Override
  public BigInteger getSwapped() {
    return swapped.getValue();
  }
}
