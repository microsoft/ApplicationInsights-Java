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

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import com.microsoft.applicationinsights.agent.internal.exporter.models.DataPointType;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MessageData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricDataPoint;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricsData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.FormattedTime;
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
  public String getId() {
    return "DeadLockDetector";
  }

  @Override
  public void report(TelemetryClient telemetryClient) {
    TelemetryItem telemetry = new TelemetryItem();
    MetricsData data = new MetricsData();
    MetricDataPoint point = new MetricDataPoint();
    TelemetryClient.getActive().initMetricTelemetry(telemetry, data, point);

    point.setName(METRIC_NAME);
    point.setValue(0);
    point.setDataPointType(DataPointType.MEASUREMENT);

    long[] threadIds = threadBean.findDeadlockedThreads();
    int blockedThreadCount = threadIds == null ? 0 : threadIds.length;

    data.getMetrics().get(0).setValue(blockedThreadCount);
    telemetry.setTime(FormattedTime.fromNow());

    telemetryClient.trackAsync(telemetry);

    if (blockedThreadCount > 0) {
      sendDetailedMessage(telemetryClient, threadIds);
    }
  }

  private void sendDetailedMessage(TelemetryClient telemetryClient, long[] threadIds) {

    TelemetryItem messageTelemetry = new TelemetryItem();
    MessageData messageData = new MessageData();
    TelemetryClient.getActive().initMessageTelemetry(messageTelemetry, messageData);

    StringBuilder sb = new StringBuilder("Suspected deadlocked threads: ");
    for (long threadId : threadIds) {
      ThreadInfo threadInfo = threadBean.getThreadInfo(threadId, MAX_STACK_TRACE);
      if (threadInfo != null) {
        appendThreadInfoAndStack(sb, threadInfo);
      }
    }
    messageData.setMessage(sb.toString());
    messageTelemetry.setTime(FormattedTime.fromNow());
    telemetryClient.trackAsync(messageTelemetry);
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
