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

package com.microsoft.applicationinsights.agent.internal.wascore.perfcounter.jvm;

import static java.lang.Math.min;

import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;
import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MessageData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.wascore.FormattedTime;
import com.microsoft.applicationinsights.agent.internal.wascore.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.wascore.perfcounter.PerformanceCounter;
import com.microsoft.applicationinsights.agent.internal.wascore.util.LocalStringsUtils;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class uses the JVM ThreadMXBean to detect threads dead locks A metric with value 0 is sent
 * when there are no blocked threads.
 *
 * <p>Otherwise the number of detected blocked threads is sent with a dimension that holds
 * information like thread id and minimal stack traces as trace telemetries.
 */
public final class DeadLockDetectorPerformanceCounter implements PerformanceCounter {

  private static final Logger logger =
      LoggerFactory.getLogger(DeadLockDetectorPerformanceCounter.class);

  private static final String INDENT = "    ";
  private static final String SEPERATOR = " | ";
  private static final String METRIC_NAME = "Suspected Deadlocked Threads";
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
    if (threadIds != null && threadIds.length > 0) {
      ArrayList<Long> blockedThreads = new ArrayList<>();

      StringBuilder sb = new StringBuilder();
      for (long threadId : threadIds) {
        ThreadInfo threadInfo = threadBean.getThreadInfo(threadId);
        if (threadInfo == null) {
          continue;
        }

        setThreadInfoAndStack(sb, threadInfo);
        blockedThreads.add(threadId);
      }

      if (!blockedThreads.isEmpty()) {
        String uuid = LocalStringsUtils.generateRandomIntegerId();

        data.getMetrics().get(0).setValue(blockedThreads.size());
        telemetry.getTags().put(ContextTagKeys.AI_OPERATION_ID.toString(), uuid);

        TelemetryItem messageTelemetry = new TelemetryItem();
        MessageData messageData = new MessageData();
        TelemetryClient.getActive().initMessageTelemetry(messageTelemetry, messageData);

        messageData.setMessage(String.format("%s%s", "Suspected deadlocked threads: ", sb));

        messageTelemetry.setTime(FormattedTime.fromNow());
        messageTelemetry.getTags().put(ContextTagKeys.AI_OPERATION_ID.toString(), uuid);

        telemetryClient.trackAsync(messageTelemetry);
      }
    }

    telemetry.setTime(FormattedTime.fromNow());

    telemetryClient.trackAsync(telemetry);
  }

  private static void setThreadInfoAndStack(StringBuilder sb, ThreadInfo ti) {
    try {
      setThreadInfo(sb, ti);

      // Stack traces up to depth of MAX_STACK_TRACE
      StackTraceElement[] stacktrace = ti.getStackTrace();
      MonitorInfo[] monitors = ti.getLockedMonitors();
      int maxTraceToReport = min(MAX_STACK_TRACE, stacktrace.length);
      for (int i = 0; i < maxTraceToReport; i++) {
        StackTraceElement ste = stacktrace[i];
        sb.append(INDENT + "at ").append(ste);
        for (MonitorInfo mi : monitors) {
          if (mi.getLockedStackDepth() == i) {
            sb.append(INDENT + "  - is locked ").append(mi);
          }
        }
      }
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable t) {
      try {
        logger.error("Error while setting the Thread Info");
        logger.trace("Error while setting the Thread Info", t);
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t2) {
        // chomp
      }
    }
    sb.append(SEPERATOR);
  }

  private static void setThreadInfo(StringBuilder sb, ThreadInfo ti) {
    sb.append(ti.getThreadName());
    sb.append(" Id=");
    sb.append(ti.getThreadId());
    sb.append(" is in ");
    sb.append(ti.getThreadState());
    if (ti.getLockName() != null) {
      sb.append(" on lock=").append(ti.getLockName());
    }
    if (ti.isSuspended()) {
      sb.append(" (suspended)");
    }
    if (ti.isInNative()) {
      sb.append(" (running in native)");
    }
    if (ti.getLockOwnerName() != null) {
      sb.append(INDENT + " is owned by ")
          .append(ti.getLockOwnerName())
          .append(" Id=")
          .append(ti.getLockOwnerId());
    }
  }
}
