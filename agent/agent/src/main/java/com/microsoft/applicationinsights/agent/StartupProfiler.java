/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
