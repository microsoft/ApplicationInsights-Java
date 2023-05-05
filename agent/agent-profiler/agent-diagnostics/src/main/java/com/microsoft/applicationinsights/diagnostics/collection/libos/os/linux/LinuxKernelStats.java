// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import com.microsoft.applicationinsights.diagnostics.collection.libos.BigIncrementalCounter;
import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.TwoStepUpdatable;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelCounters;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelStatsReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** Scrapes data from /proc/stat */
public class LinuxKernelStats implements KernelStatsReader, TwoStepUpdatable, Closeable {

  private static final int NOTHING_READ = -1;

  private static final Charset PROC_CHARSET = StandardCharsets.US_ASCII;
  private static final String PROC_STAT = "/proc/stat";

  public static final String PROCS_RUNNING = "procs_running";
  public static final String PROCS_BLOCKED = "procs_blocked";
  public static final String CTXT = "ctxt";

  private static final int PROCS_RUNNABLE_OFFSET = PROCS_RUNNING.length() + 1;
  private static final int PROCS_BLOCKED_OFFSET = PROCS_BLOCKED.length() + 1;
  private static final int CONTEXT_SWITCH_OFFSET = CTXT.length() + 1;
  private int bufferSize = 1024 * 3;

  private final RandomAccessFile procFile;
  private byte[] buff;
  private int amountRead = NOTHING_READ;

  private final BigIncrementalCounter contextSwitches = new BigIncrementalCounter();
  private final BigIncrementalCounter userTime = new BigIncrementalCounter();
  private final BigIncrementalCounter systemTime = new BigIncrementalCounter();
  private final BigIncrementalCounter idleTime = new BigIncrementalCounter();
  private final BigIncrementalCounter waitTime = new BigIncrementalCounter();

  private int procsRunnableLineOffset = 0;
  private int procsBlockedLineOffset = 0;
  private int ctxLineOffset = 0;

  private KernelCounters counters;

  public LinuxKernelStats() {
    this(PROC_STAT);
  }

  public LinuxKernelStats(String statFile) {
    try {
      procFile = new RandomAccessFile(statFile, "r");
      generateBuffer();
    } catch (FileNotFoundException | OperatingSystemInteractionException e) {
      throw new IllegalStateException(e);
    }
  }

  private void generateBuffer() throws OperatingSystemInteractionException {
    buff = new byte[bufferSize];
    poll();
    while (amountRead == bufferSize) {
      bufferSize += 1024;
      buff = new byte[bufferSize];
      poll();
    }
  }

  @Override
  public KernelCounters getCounters() {
    return counters;
  }

  @Override
  public void poll() throws OperatingSystemInteractionException {
    try {
      procFile.seek(0);
      amountRead = procFile.read(buff);
    } catch (IOException e) {
      throw new OperatingSystemInteractionException("Error reading kernel counters", e);
    }
  }

  @Override
  public void close() throws IOException {
    procFile.close();
  }

  @Override
  public void update() throws OperatingSystemInteractionException {
    if (amountRead != NOTHING_READ) {
      String[] procStatContents = new String(buff, 0, amountRead, PROC_CHARSET).split("\n");

      String[] cpuSplit = procStatContents[0].split(" ");
      userTime.newValue(cpuSplit[2]);
      // skip user niced
      systemTime.newValue(cpuSplit[4]);
      idleTime.newValue(cpuSplit[5]);
      // TODO: optional
      waitTime.newValue(cpuSplit[6]);

      if (procsRunnableLineOffset == 0) {
        procsRunnableLineOffset = lineIndexFor(procStatContents, PROCS_RUNNING);
      }
      if (procsBlockedLineOffset == 0) {
        procsBlockedLineOffset = lineIndexFor(procStatContents, PROCS_BLOCKED);
      }
      if (ctxLineOffset == 0) {
        ctxLineOffset = lineIndexFor(procStatContents, CTXT);
      }

      String contextSwitchString = procStatContents[ctxLineOffset];
      contextSwitches.newValue(contextSwitchString.substring(CONTEXT_SWITCH_OFFSET));

      String runnableString = procStatContents[procsRunnableLineOffset];
      long procsRunnable = Long.parseLong(runnableString.substring(PROCS_RUNNABLE_OFFSET));
      String blockedString = procStatContents[procsBlockedLineOffset];
      long procsBlocked = Long.parseLong(blockedString.substring(PROCS_BLOCKED_OFFSET));

      updateCounter(procsRunnable, procsBlocked);
    } else {
      throw new OperatingSystemInteractionException("Must poll before parsing");
    }
  }

  @SuppressWarnings({"AvoidObjectArrays"})
  public static int lineIndexFor(String[] procStatContents, String lineContents) {
    int index = 0;
    while (index < procStatContents.length && !procStatContents[index].contains(lineContents)) {
      index++;
    }
    return index;
  }

  private void updateCounter(long procsRunnable, long procsBlocked) {
    long incrementInSystemTime = systemTime.getNonNullIncrement().longValue();
    long incrementInUserTime = userTime.getNonNullIncrement().longValue();
    long incrementInIdleTime = idleTime.getNonNullIncrement().longValue();
    long incrementInWaitTime = waitTime.getNonNullIncrement().longValue();

    long totalTime =
        incrementInSystemTime + incrementInUserTime + incrementInIdleTime + incrementInWaitTime;
    double factor = 100.0 / totalTime;

    incrementInSystemTime = normalize(incrementInSystemTime, factor);
    incrementInUserTime = normalize(incrementInUserTime, factor);
    incrementInIdleTime = normalize(incrementInIdleTime, factor);
    incrementInWaitTime = normalize(incrementInWaitTime, factor);

    this.counters =
        new KernelCounters(
            contextSwitches.getNonNullIncrement().longValue(),
            incrementInUserTime,
            incrementInSystemTime,
            incrementInIdleTime,
            incrementInWaitTime,
            procsRunnable,
            procsBlocked);
  }

  private static long normalize(long incrementInSystemTime, double factor) {
    return (long) (incrementInSystemTime * factor);
  }
}
