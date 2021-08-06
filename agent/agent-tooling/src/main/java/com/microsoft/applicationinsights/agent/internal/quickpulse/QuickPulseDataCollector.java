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

package com.microsoft.applicationinsights.agent.internal.quickpulse;

import com.microsoft.applicationinsights.agent.internal.exporter.models.MonitorDomain;
import com.microsoft.applicationinsights.agent.internal.exporter.models.RemoteDependencyData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.RequestData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryExceptionData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.perfcounter.CpuPerformanceCounterCalculator;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.LoggerFactory;

public enum QuickPulseDataCollector {
  INSTANCE;

  private TelemetryClient telemetryClient;

  static class FinalCounters {
    public final int exceptions;
    public final long requests;
    public final double requestsDuration;
    public final int unsuccessfulRequests;
    public final long rdds;
    public final double rddsDuration;
    public final int unsuccessfulRdds;
    public final long memoryCommitted;
    public final double cpuUsage;

    public FinalCounters(
        Counters currentCounters,
        MemoryMXBean memory,
        CpuPerformanceCounterCalculator cpuPerformanceCounterCalculator) {
      if (memory != null && memory.getHeapMemoryUsage() != null) {
        memoryCommitted = memory.getHeapMemoryUsage().getCommitted();
      } else {
        memoryCommitted = -1;
      }

      Double cpuDatum;
      if (cpuPerformanceCounterCalculator != null
          && (cpuDatum = cpuPerformanceCounterCalculator.getProcessCpuUsage()) != null) {
        // normally I wouldn't do this, but I prefer to avoid code duplication more than one-liners
        // :)
        cpuUsage = cpuDatum;
      } else {
        cpuUsage = -1;
      }
      exceptions = currentCounters.exceptions.get();

      CountAndDuration countAndDuration =
          Counters.decodeCountAndDuration(currentCounters.requestsAndDurations.get());
      requests = countAndDuration.count;
      this.requestsDuration = countAndDuration.duration;
      this.unsuccessfulRequests = currentCounters.unsuccessfulRequests.get();

      countAndDuration = Counters.decodeCountAndDuration(currentCounters.rddsAndDuations.get());
      this.rdds = countAndDuration.count;
      this.rddsDuration = countAndDuration.duration;
      this.unsuccessfulRdds = currentCounters.unsuccessfulRdds.get();
    }
  }

  static class CountAndDuration {
    public final long count;
    public final long duration;

    private CountAndDuration(long count, long duration) {
      this.count = count;
      this.duration = duration;
    }
  }

  static class Counters {
    private static final long MAX_COUNT = 524287L;
    private static final long MAX_DURATION = 17592186044415L;

    public final AtomicInteger exceptions = new AtomicInteger(0);

    final AtomicLong requestsAndDurations = new AtomicLong(0);
    final AtomicInteger unsuccessfulRequests = new AtomicInteger(0);

    final AtomicLong rddsAndDuations = new AtomicLong(0);
    final AtomicInteger unsuccessfulRdds = new AtomicInteger(0);

    static long encodeCountAndDuration(long count, long duration) {
      if (count > MAX_COUNT || duration > MAX_DURATION) {
        return 0;
      }

      return (count << 44) + duration;
    }

    static CountAndDuration decodeCountAndDuration(long countAndDuration) {
      return new CountAndDuration(countAndDuration >> 44, countAndDuration & MAX_DURATION);
    }
  }

  private final AtomicReference<Counters> counters = new AtomicReference<>(null);
  private final MemoryMXBean memory;
  private final CpuPerformanceCounterCalculator cpuPerformanceCounterCalculator;

  QuickPulseDataCollector() {
    CpuPerformanceCounterCalculator temp;
    try {
      temp = new CpuPerformanceCounterCalculator();
    } catch (ThreadDeath td) {
      throw td;
    } catch (Throwable t) {
      try {
        LoggerFactory.getLogger(QuickPulseDataCollector.class)
            .error(
                "Could not initialize {}",
                CpuPerformanceCounterCalculator.class.getSimpleName(),
                t);
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t2) {
        // chomp
      }
      temp = null;
    }
    cpuPerformanceCounterCalculator = temp;
    memory = ManagementFactory.getMemoryMXBean();
  }

  public synchronized void disable() {
    counters.set(null);
  }

  public synchronized void enable(TelemetryClient telemetryClient) {
    this.telemetryClient = telemetryClient;
    counters.set(new Counters());
  }

  public synchronized FinalCounters getAndRestart() {
    Counters currentCounters = counters.getAndSet(new Counters());
    if (currentCounters != null) {
      return new FinalCounters(currentCounters, memory, cpuPerformanceCounterCalculator);
    }

    return null;
  }

  /*@VisibleForTesting*/
  synchronized FinalCounters peek() {
    Counters currentCounters = this.counters.get(); // this should be the only differece
    if (currentCounters != null) {
      return new FinalCounters(currentCounters, memory, cpuPerformanceCounterCalculator);
    }
    return null;
  }

  public void add(TelemetryItem telemetryItem) {
    if (telemetryClient == null) {
      // quick pulse is not enabled
      return;
    }

    if (!telemetryItem.getInstrumentationKey().equals(getInstrumentationKey())) {
      return;
    }

    Float sampleRate = telemetryItem.getSampleRate();
    int itemCount = (sampleRate == null || sampleRate == 0) ? 1 : Math.round(100 / sampleRate);

    MonitorDomain data = telemetryItem.getData().getBaseData();
    if (data instanceof RequestData) {
      RequestData requestTelemetry = (RequestData) data;
      addRequest(requestTelemetry, itemCount);
    } else if (data instanceof RemoteDependencyData) {
      addDependency((RemoteDependencyData) data, itemCount);
    } else if (data instanceof TelemetryExceptionData) {
      addException(itemCount);
    }
  }

  private synchronized String getInstrumentationKey() {
    return telemetryClient.getInstrumentationKey();
  }

  private void addDependency(RemoteDependencyData telemetry, int itemCount) {
    Counters counters = this.counters.get();
    if (counters == null) {
      return;
    }
    counters.rddsAndDuations.addAndGet(
        Counters.encodeCountAndDuration(itemCount, parseDurationToMillis(telemetry.getDuration())));
    Boolean success = telemetry.isSuccess();
    if (success != null && !success) { // success should not be null
      counters.unsuccessfulRdds.incrementAndGet();
    }
  }

  private void addException(int itemCount) {
    Counters counters = this.counters.get();
    if (counters == null) {
      return;
    }

    counters.exceptions.addAndGet(itemCount);
  }

  private void addRequest(RequestData requestTelemetry, int itemCount) {
    Counters counters = this.counters.get();
    if (counters == null) {
      return;
    }

    counters.requestsAndDurations.addAndGet(
        Counters.encodeCountAndDuration(
            itemCount, parseDurationToMillis(requestTelemetry.getDuration())));
    if (!requestTelemetry.isSuccess()) {
      counters.unsuccessfulRequests.incrementAndGet();
    }
  }

  // TODO (trask) optimization: move live metrics request capture to OpenTelemetry layer so don't
  // have to parse String duration
  // visible for testing
  static long parseDurationToMillis(String duration) {
    // format is DD.HH:MM:SS.MMMMMM
    return startingAtDaysOrHours(duration);
  }

  private static long startingAtDaysOrHours(String duration) {
    int i = 0;
    char c = duration.charAt(i++);
    long daysOrHours = charToInt(c);

    c = duration.charAt(i++);
    while (c != ':' && c != '.') {
      daysOrHours = 10 * daysOrHours + charToInt(c);
      c = duration.charAt(i++);
    }
    if (c == ':') {
      // was really hours
      return startingAtMinutes(duration, i, daysOrHours);
    } else {
      return startingAtHours(duration, i, daysOrHours);
    }
  }

  private static long startingAtHours(String duration, int i, long runningTotalInDays) {
    char c1 = duration.charAt(i++);
    char c2 = duration.charAt(i++);
    int hours = 10 * charToInt(c1) + charToInt(c2);
    return startingAtMinutes(duration, i + 1, 24 * runningTotalInDays + hours);
  }

  private static long startingAtMinutes(String duration, int i, long runningTotalInHours) {
    char c1 = duration.charAt(i++);
    char c2 = duration.charAt(i++);
    int minutes = 10 * charToInt(c1) + charToInt(c2);
    // next char must be ':'
    return startingAtSeconds(duration, i + 1, 60 * runningTotalInHours + minutes);
  }

  private static long startingAtSeconds(String duration, int i, long runningTotalInMinutes) {
    char c1 = duration.charAt(i++);
    char c2 = duration.charAt(i++);
    int seconds = 10 * charToInt(c1) + charToInt(c2);
    return startingAtMicros(duration, i + 1, 60 * runningTotalInMinutes + seconds);
  }

  private static long startingAtMicros(String duration, int i, long runningTotalInSeconds) {
    int millis = 0;
    // only care about milliseconds
    for (int j = i; j < i + 3; j++) {
      char c = duration.charAt(j);
      millis = 10 * millis + charToInt(c);
    }
    return 1000 * runningTotalInSeconds + millis;
  }

  private static int charToInt(char c) {
    int x = c - '0';
    if (x < 0 || x > 9) {
      throw new AssertionError("Unexpected char '" + c + "'");
    }
    return x;
  }
}
