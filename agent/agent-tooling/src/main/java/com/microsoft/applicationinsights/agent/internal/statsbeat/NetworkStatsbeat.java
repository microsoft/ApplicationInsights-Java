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

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.StatsbeatTelemetryBuilder;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.instrumentation.api.internal.GuardedBy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class NetworkStatsbeat extends BaseStatsbeat {

  private static final String REQUEST_SUCCESS_COUNT_METRIC_NAME = "Request Success Count";
  private static final String REQUEST_FAILURE_COUNT_METRIC_NAME = "Request Failure Count";
  private static final String REQUEST_DURATION_METRIC_NAME = "Request Duration";
  private static final String RETRY_COUNT_METRIC_NAME = "Retry Count";
  private static final String THROTTLE_COUNT_METRIC_NAME = "Throttle Count";
  private static final String EXCEPTION_COUNT_METRIC_NAME = "Exception Count";
  private static final String BREEZE_ENDPOINT = "breeze";

  private static final Pattern hostPattern = Pattern.compile("^https?://(?:www\\.)?([^/.]+)");

  private final Object lock = new Object();

  @GuardedBy("lock")
  private final Map<IntervalMetricsKey, IntervalMetrics> instrumentationKeyCounterMap = new HashMap<>();

  // only used by tests
  public NetworkStatsbeat() {
    super(new CustomDimensions());
  }

  public NetworkStatsbeat(CustomDimensions customDimensions) {
    super(customDimensions);
  }

  @Override
  protected void send(TelemetryClient telemetryClient) {
    Map<IntervalMetricsKey, IntervalMetrics> local;
    synchronized (lock) {
      local = new HashMap<>(instrumentationKeyCounterMap);
      instrumentationKeyCounterMap.clear();
    }

    for (Map.Entry<IntervalMetricsKey, IntervalMetrics> entry : local.entrySet()) {
      IntervalMetricsKey key = entry.getKey();
      sendIntervalMetric(telemetryClient, key, entry.getValue());
    }
  }

  public void incrementRequestSuccessCount(long duration, String ikey, String host) {
    doWithIntervalMetrics(
        ikey,
        host,
        null,
        intervalMetrics -> {
          intervalMetrics.requestSuccessCount.incrementAndGet();
          intervalMetrics.totalRequestDuration.getAndAdd(duration);
        });
  }

  public void incrementRequestFailureCount(String ikey, String host, Integer statusCode) {
    doWithIntervalMetrics(
        ikey,
        host,
        statusCode,
        intervalMetrics -> intervalMetrics.requestFailureCount.incrementAndGet());
  }

  // TODO (heya) this is never called
  public void incrementRetryCount(String ikey, String host, Integer statusCode) {
    doWithIntervalMetrics(
        ikey, host, statusCode, intervalMetrics -> intervalMetrics.retryCount.incrementAndGet());
  }

  public void incrementThrottlingCount(String ikey, String host, Integer statusCode) {
    doWithIntervalMetrics(
        ikey,
        host,
        statusCode,
        intervalMetrics -> intervalMetrics.throttlingCount.incrementAndGet());
  }

  void incrementExceptionCount(String ikey, String host, String exceptionType) {
    doWithIntervalMetrics(
        ikey,
        host,
        exceptionType,
        intervalMetrics -> intervalMetrics.exceptionCount.incrementAndGet());
  }

  // only used by tests
  long getRequestSuccessCount(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(new IntervalMetricsKey(ikey, null));
      return intervalMetrics == null ? 0L : intervalMetrics.requestSuccessCount.get();
    }
  }

  // only used by tests
  long getRequestFailureCount(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(new IntervalMetricsKey(ikey, null));
      return intervalMetrics == null ? 0L : intervalMetrics.requestFailureCount.get();
    }
  }

  // only used by tests
  double getRequestDurationAvg(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(new IntervalMetricsKey(ikey, null));
      return intervalMetrics == null ? 0L : intervalMetrics.getRequestDurationAvg();
    }
  }

  // only used by tests
  long getRetryCount(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(new IntervalMetricsKey(ikey, null));
      return intervalMetrics == null ? 0L : intervalMetrics.retryCount.get();
    }
  }

  // only used by tests
  long getThrottlingCount(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(new IntervalMetricsKey(ikey, null));
      return intervalMetrics == null ? 0L : intervalMetrics.throttlingCount.get();
    }
  }

  // only used by tests
  long getExceptionCount(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(new IntervalMetricsKey(ikey, null));
      return intervalMetrics == null ? 0L : intervalMetrics.exceptionCount.get();
    }
  }

  private void doWithIntervalMetrics(
      String ikey, String host, @Nullable Object cause, Consumer<IntervalMetrics> update) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics =
          instrumentationKeyCounterMap.computeIfAbsent(new IntervalMetricsKey(ikey, cause), k -> new IntervalMetrics());
      intervalMetrics.host = host;
      update.accept(intervalMetrics);
    }
  }

  private void sendIntervalMetric(
      TelemetryClient telemetryClient, IntervalMetricsKey key, IntervalMetrics local) {
    if (local.requestSuccessCount.get() != 0) {
      StatsbeatTelemetryBuilder requestSuccessCountSt =
          createStatsbeatTelemetry(
              telemetryClient,
              REQUEST_SUCCESS_COUNT_METRIC_NAME,
              (double) local.requestSuccessCount.get());
      addCommonProperties(requestSuccessCountSt, key.ikey, local.host, key.cause);
      telemetryClient.trackStatsbeatAsync(requestSuccessCountSt.build());
    }

    if (local.requestFailureCount.get() != 0) {
      StatsbeatTelemetryBuilder requestFailureCountSt =
          createStatsbeatTelemetry(
              telemetryClient,
              REQUEST_FAILURE_COUNT_METRIC_NAME,
              (double) local.requestFailureCount.get());
      addCommonProperties(requestFailureCountSt, key.ikey, local.host, key.cause);
      telemetryClient.trackStatsbeatAsync(requestFailureCountSt.build());
    }

    double durationAvg = local.getRequestDurationAvg();
    if (durationAvg != 0) {
      StatsbeatTelemetryBuilder requestDurationSt =
          createStatsbeatTelemetry(telemetryClient, REQUEST_DURATION_METRIC_NAME, durationAvg);
      addCommonProperties(requestDurationSt, key.ikey, local.host, key.cause);
      telemetryClient.trackStatsbeatAsync(requestDurationSt.build());
    }

    if (local.retryCount.get() != 0) {
      StatsbeatTelemetryBuilder retryCountSt =
          createStatsbeatTelemetry(
              telemetryClient, RETRY_COUNT_METRIC_NAME, (double) local.retryCount.get());
      addCommonProperties(retryCountSt, key.ikey, local.host, key.cause);
      telemetryClient.trackStatsbeatAsync(retryCountSt.build());
    }

    if (local.throttlingCount.get() != 0) {
      StatsbeatTelemetryBuilder throttleCountSt =
          createStatsbeatTelemetry(
              telemetryClient, THROTTLE_COUNT_METRIC_NAME, (double) local.throttlingCount.get());
      addCommonProperties(throttleCountSt, key.ikey, local.host, key.cause);
      telemetryClient.trackStatsbeatAsync(throttleCountSt.build());
    }

    if (local.exceptionCount.get() != 0) {
      StatsbeatTelemetryBuilder exceptionCountSt =
          createStatsbeatTelemetry(
              telemetryClient, EXCEPTION_COUNT_METRIC_NAME, (double) local.exceptionCount.get());
      addCommonProperties(exceptionCountSt, key.ikey, local.host, key.cause);
      telemetryClient.trackStatsbeatAsync(exceptionCountSt.build());
    }
  }

  private static void addCommonProperties(
      StatsbeatTelemetryBuilder telemetryBuilder, String ikey, String host, @Nullable Object cause) {
    telemetryBuilder.addProperty("endpoint", BREEZE_ENDPOINT);
    telemetryBuilder.addProperty("cikey", ikey);
    telemetryBuilder.addProperty("host", host);
    if (cause != null) {
      // track 'statusCode' for Failure/Retry/Throttle count and track 'exceptionType' for Exception
      // Count
      if (cause instanceof Integer) {
        telemetryBuilder.addProperty("statusCode", ((Integer) cause).toString());
      } else if (cause instanceof String) {
        telemetryBuilder.addProperty("exceptionType", (String) cause);
      }
    }
  }

  private static class IntervalMetricsKey {
    private final String ikey;
    // cause can be an integer for statusCode and a string for exceptionType
    private final Object cause;

    IntervalMetricsKey(String ikey, Object cause) {
      this.ikey = ikey;
      this.cause = cause;
    }
  }

  private static class IntervalMetrics {
    private final AtomicLong requestSuccessCount = new AtomicLong();
    private final AtomicLong requestFailureCount = new AtomicLong();
    // request duration count only counts request success.
    private final AtomicLong totalRequestDuration = new AtomicLong(); // duration in milliseconds
    private final AtomicLong retryCount = new AtomicLong();
    private final AtomicLong throttlingCount = new AtomicLong();
    private final AtomicLong exceptionCount = new AtomicLong();

    private volatile String host;

    private double getRequestDurationAvg() {
      double sum = totalRequestDuration.get();
      if (requestSuccessCount.get() != 0) {
        return sum / requestSuccessCount.get();
      }

      return sum;
    }
  }

  /**
   * e.g. endpointUrl 'https://westus-0.in.applicationinsights.azure.com/v2.1/track' host will
   * return 'westus-0'
   */
  static String getHost(String endpointUrl) {
    Matcher matcher = hostPattern.matcher(endpointUrl);

    if (matcher.find()) {
      return matcher.group(1);
    }

    // it's better to send bad endpointUrl to Statsbeat for troubleshooting.
    return endpointUrl;
  }
}
