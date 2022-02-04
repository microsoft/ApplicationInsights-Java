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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

final class StartupProfiler {

  private static final String STACKTRACES = "stacktrace.txt";

  @SuppressWarnings("SystemOut")
  public static void start() {
    if (isReadonly()) {
      System.out.println("It's a readonly file system. StartupProfiler does nothing in this case.");
      return;
    }

    // this is used to support -Dapplicationinsights.debug.startupProfiling=true
    boolean startupProfilingEnabled =
        Boolean.parseBoolean(System.getProperty("applicationinsights.debug.startupProfiling"));
    if (!startupProfilingEnabled) {
      System.out.println(
          "Exit StartupProfiler - system property 'applicationinsights.debug.startupProfiling' is not set to true.");
      return;
    }

    String tempDirectory = System.getProperty("java.io.tmpdir");
    File folder = new File(tempDirectory, "applicationinsights");
    if (!folder.exists()) {
      folder.mkdirs();
    }

    File dumpFile = new File(folder, STACKTRACES);
    System.out.println("Create StartupProfiler dump here: '" + dumpFile.getPath() + "'");

    PrintWriter printWriter = null;
    try (PrintWriter out =
        new PrintWriter(Files.newBufferedWriter(dumpFile.toPath(), Charset.defaultCharset()))) {
      printWriter = new PrintWriter(out);
      start(printWriter);
    } catch (IOException e) {
      System.out.println(
          "Error occurs when writing dump to " + STACKTRACES + " \ncause: " + e.getCause());
    } finally {
      if (printWriter != null) {
        printWriter.close();
      }
    }
  }

  private static void start(PrintWriter out) {
    Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(new ThreadDump(out), 50, 50, TimeUnit.MILLISECONDS);
  }

  private static boolean isReadonly() {
    File tempDir = new File(System.getProperty("java.io.tmpdir"));
    return tempDir.canRead() && !tempDir.canWrite();
  }

  private StartupProfiler() {}

  private static class ThreadDump implements Runnable {

    private final PrintWriter out;

    private ThreadDump(PrintWriter out) {
      this.out = out;
    }

    @Override
    public void run() {
      out.println("========================================");
      RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
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
      if (capture(threadInfo)) {}
      out.println(threadInfo.getThreadName() + " #" + threadInfo.getThreadId());
      out.println("   java.lang.Thread.State: " + threadInfo.getThreadState());
      for (StackTraceElement ste : threadInfo.getStackTrace()) {
        out.println("        " + ste);
      }
      out.println();
    }

    private static boolean capture(ThreadInfo threadInfo) {
      if (threadInfo.getThreadName().equals("main")) {
        return true;
      }
      // check stack trace length helps to skip "Signal Dispatcher" thread
      return threadInfo.getThreadState() == Thread.State.RUNNABLE
          && threadInfo.getStackTrace().length > 0;
    }
  }
}
