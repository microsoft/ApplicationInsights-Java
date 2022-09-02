// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.MessageTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedTime;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * The class uses the JVM ThreadMXBean to detect thread deadlocks.
 *
 * <p>A metric with value 0 is sent when there are no blocked threads.
 *
 * <p>Otherwise the number of detected blocked threads is sent, along with minimal stack traces as
 * trace telemetries.
 */
public final class DeadLockDetectorPerformanceCounter implements PerformanceCounter {

  private static final String METRIC_NAME = "Suspected Deadlocked Threads";

  // TODO (trask) this seems low..
  private static final int MAX_STACK_TRACE = 3;

  private final ThreadMXBean threadBean;

  public DeadLockDetectorPerformanceCounter() {
    threadBean = ManagementFactory.getThreadMXBean();
  }

  @Override
  public void report(TelemetryClient telemetryClient) {

    long[] threadIds = threadBean.findDeadlockedThreads();
    int blockedThreadCount = threadIds == null ? 0 : threadIds.length;

    telemetryClient.trackAsync(telemetryClient.newMetricTelemetry(METRIC_NAME, blockedThreadCount));

    if (blockedThreadCount > 0) {
      sendDetailedMessage(telemetryClient, threadIds);
    }
  }

  private void sendDetailedMessage(TelemetryClient telemetryClient, long[] threadIds) {

    MessageTelemetryBuilder telemetryBuilder = telemetryClient.newMessageTelemetryBuilder();

    StringBuilder sb = new StringBuilder("Suspected deadlocked threads: ");
    for (long threadId : threadIds) {
      ThreadInfo threadInfo = threadBean.getThreadInfo(threadId, MAX_STACK_TRACE);
      if (threadInfo != null) {
        appendThreadInfoAndStack(sb, threadInfo);
      }
    }
    telemetryBuilder.setMessage(sb.toString());
    telemetryBuilder.setTime(FormattedTime.offSetDateTimeFromNow());
    telemetryClient.trackAsync(telemetryBuilder.build());
  }

  private static void appendThreadInfoAndStack(StringBuilder sb, ThreadInfo threadInfo) {
    setThreadInfo(sb, threadInfo);

    StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
    MonitorInfo[] monitorInfos = threadInfo.getLockedMonitors();
    for (int i = 0; i < stackTraceElements.length; i++) {
      StackTraceElement ste = stackTraceElements[i];
      sb.append("\n    at ").append(ste);
      for (MonitorInfo monitorInfo : monitorInfos) {
        if (monitorInfo.getLockedStackDepth() == i) {
          sb.append("\n      - is locked ").append(monitorInfo);
        }
      }
    }
  }

  private static void setThreadInfo(StringBuilder sb, ThreadInfo threadInfo) {
    sb.append("\n  ");
    sb.append(threadInfo.getThreadName());
    sb.append(" Id=");
    sb.append(threadInfo.getThreadId());
    sb.append(" is in ");
    sb.append(threadInfo.getThreadState());
    if (threadInfo.getLockName() != null) {
      sb.append(" on lock=").append(threadInfo.getLockName());
    }
    if (threadInfo.isSuspended()) {
      sb.append(" (suspended)");
    }
    if (threadInfo.isInNative()) {
      sb.append(" (running in native)");
    }
    if (threadInfo.getLockOwnerName() != null) {
      sb.append(" is owned by ")
          .append(threadInfo.getLockOwnerName())
          .append(" Id=")
          .append(threadInfo.getLockOwnerId());
    }
  }
}
