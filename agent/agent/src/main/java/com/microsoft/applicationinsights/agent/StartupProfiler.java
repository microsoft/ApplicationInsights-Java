// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.Charset;
import java.nio.file.Files;

final class StartupProfiler {

  private static final String STACKTRACES = "stacktrace.txt";

  @SuppressWarnings("SystemOut")
  public static void start() {
    String tempDirectory = System.getProperty("java.io.tmpdir");
    File folder = new File(tempDirectory, "applicationinsights");
    if (!folder.exists() && !folder.mkdirs()) {
      System.out.println("Failed to create directory: " + tempDirectory);
      return;
    }

    File dumpFile = new File(folder, STACKTRACES);
    System.out.println("Writing startup profiler to '" + dumpFile.getPath() + "'");

    PrintWriter printWriter;
    try {
      printWriter =
          new PrintWriter(Files.newBufferedWriter(dumpFile.toPath(), Charset.defaultCharset()));
    } catch (IOException e) {
      System.out.println("Error occurred when writing dump to " + dumpFile.getPath());
      e.printStackTrace();
      return;
    }

    start(printWriter);
  }

  private static void start(PrintWriter out) {
    Thread thread = new Thread(new ThreadDump(out), "StartupProfiler");
    thread.setDaemon(true);
    thread.start();
  }

  private static class ThreadDump implements Runnable {

    private final PrintWriter out;

    private ThreadDump(PrintWriter out) {
      this.out = out;
    }

    @Override
    @SuppressWarnings("SystemOut")
    public void run() {
      long start = System.currentTimeMillis();
      while (System.currentTimeMillis() - start < MINUTES.toMillis(10)) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          System.out.println("Startup profiler interrupted");
          return;
        }
        captureThreadDump();
      }
    }

    private void captureThreadDump() {
      out.println("========================================");
      RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
      out.print("uptime: ");
      out.println(runtimeBean.getUptime());
      out.println();
      ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
      ThreadInfo[] threadInfos =
          threadBean.getThreadInfo(
              threadBean.getAllThreadIds(), threadBean.isObjectMonitorUsageSupported(), false);
      long currentThreadId = Thread.currentThread().getId();
      for (ThreadInfo threadInfo : threadInfos) {
        // If a thread of a given ID is not alive or does not exist, the corresponding element in
        // the returned array will contain null
        if (threadInfo == null) {
          continue;
        }

        if (threadInfo.getThreadId() != currentThreadId) {
          write(threadInfo);
        }
      }
      out.flush();
    }

    private void write(ThreadInfo threadInfo) {
      out.println(threadInfo.getThreadName() + " #" + threadInfo.getThreadId());
      out.println("   java.lang.Thread.State: " + threadInfo.getThreadState());
      for (StackTraceElement ste : threadInfo.getStackTrace()) {
        out.println("        " + ste);
      }
      out.println();
    }
  }

  private StartupProfiler() {}
}
