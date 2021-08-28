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

import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkStatsbeat extends BaseStatsbeat {

  private static final String REQUEST_SUCCESS_COUNT_METRIC_NAME = "Request Success Count";
  private static final String REQUEST_FAILURE_COUNT_METRIC_NAME = "Requests Failure Count ";
  private static final String REQUEST_DURATION_METRIC_NAME = "Request Duration";
  private static final String RETRY_COUNT_METRIC_NAME = "Retry Count";
  private static final String THROTTLE_COUNT_METRIC_NAME = "Throttle Count";
  private static final String EXCEPTION_COUNT_METRIC_NAME = "Exception Count";
  private static final String BREEZE_ENDPOINT = "breeze";

  private final Object lock = new Object();
  private final ConcurrentMap<String, IntervalMetrics> instrumentationKeyCounterMap =
      new ConcurrentHashMap<>();

  NetworkStatsbeat(CustomDimensions customDimensions) {
    super(customDimensions);
  }

  void initInstrumentationKeyCounterMap(List<String> ikeys) {
    for (String ikey : ikeys) {
      IntervalMetrics intervalMetrics = new IntervalMetrics();
      instrumentationKeyCounterMap.put(ikey, intervalMetrics);
    }
  }

  @Override
  protected void send(TelemetryClient telemetryClient) {
    IntervalMetrics local = instrumentationKeyCounterMap.get(telemetryClient.getInstrumentationKey());
    instrumentationKeyCounterMap.put(telemetryClient.getInstrumentationKey(), new IntervalMetrics());
    sendIntervalMetric(
        telemetryClient,
        local,
        getHost(telemetryClient.getEndpointProvider().getIngestionEndpointUrl().toString()));
  }

  public void incrementRequestSuccessCount(long duration, String ikey) {
    if (!Strings.isNullOrEmpty(ikey) && instrumentationKeyCounterMap.get(ikey) != null) {
      synchronized (lock) {
        instrumentationKeyCounterMap.get(ikey).requestSuccessCount.incrementAndGet();
        instrumentationKeyCounterMap.get(ikey).totalRequestDuration.getAndAdd(duration);
      }
    }
  }

  public void incrementRequestFailureCount(String ikey) {
    if (instrumentationKeyCounterMap.get(ikey) != null) {
      synchronized (lock) {
        instrumentationKeyCounterMap.get(ikey).requestFailureCount.incrementAndGet();
      }
    }
  }

  public void incrementRetryCount(String ikey) {
    if (instrumentationKeyCounterMap.get(ikey) != null) {
      synchronized (lock) {
        instrumentationKeyCounterMap.get(ikey).retryCount.incrementAndGet();
      }
    }
  }

  public void incrementThrottlingCount(String ikey) {
    if (instrumentationKeyCounterMap.get(ikey) != null) {
      synchronized (lock) {
        instrumentationKeyCounterMap.get(ikey).throttlingCount.incrementAndGet();
      }
    }
  }

  void incrementExceptionCount(String ikey) {
    if (instrumentationKeyCounterMap.get(ikey) != null) {
      synchronized (lock) {
        instrumentationKeyCounterMap.get(ikey).exceptionCount.incrementAndGet();
      }
    }
  }

  // only used by tests
  long getRequestSuccessCount(String ikey) {
    if (instrumentationKeyCounterMap.get(ikey) != null) {
      return instrumentationKeyCounterMap.get(ikey).requestSuccessCount.get();
    }
    return 0L;
  }

  // only used by tests
  long getRequestFailureCount(String ikey) {
    if (instrumentationKeyCounterMap.get(ikey) != null) {
      return instrumentationKeyCounterMap.get(ikey).requestFailureCount.get();
    }
    return 0L;
  }

  // only used by tests
  double getRequestDurationAvg(String ikey) {
    if (instrumentationKeyCounterMap.get(ikey) != null) {
      return instrumentationKeyCounterMap.get(ikey).getRequestDurationAvg();
    }
    return 0L;
  }

  // only used by tests
  long getRetryCount(String ikey) {
    if (instrumentationKeyCounterMap.get(ikey) != null) {
      return instrumentationKeyCounterMap.get(ikey).retryCount.get();
    }
    return 0L;
  }

  // only used by tests
  long getThrottlingCount(String ikey) {
    if (instrumentationKeyCounterMap.get(ikey) != null) {
      return instrumentationKeyCounterMap.get(ikey).throttlingCount.get();
    }
    return 0L;
  }

  // only used by tests
  long getExceptionCount(String ikey) {
    if (instrumentationKeyCounterMap.get(ikey) != null) {
      return instrumentationKeyCounterMap.get(ikey).exceptionCount.get();
    }
    return 0L;
  }

  private void sendIntervalMetric(
      TelemetryClient telemetryClient, IntervalMetrics local, String host) {
    if (local.requestSuccessCount.get() != 0) {
      TelemetryItem requestSuccessCountSt =
          createStatsbeatTelemetry(
              telemetryClient, REQUEST_SUCCESS_COUNT_METRIC_NAME, local.requestSuccessCount.get());
      addEndpointAndHostToProperties(requestSuccessCountSt, host);
      telemetryClient.trackStatsbeatAsync(requestSuccessCountSt);
    }

    if (local.requestFailureCount.get() != 0) {
      TelemetryItem requestFailureCountSt =
          createStatsbeatTelemetry(
              telemetryClient, REQUEST_FAILURE_COUNT_METRIC_NAME, local.requestFailureCount.get());
      addEndpointAndHostToProperties(requestFailureCountSt, host);
      telemetryClient.trackStatsbeatAsync(requestFailureCountSt);
    }

    double durationAvg = local.getRequestDurationAvg();
    if (durationAvg != 0) {
      TelemetryItem requestDurationSt =
          createStatsbeatTelemetry(telemetryClient, REQUEST_DURATION_METRIC_NAME, durationAvg);
      addEndpointAndHostToProperties(requestDurationSt, host);
      telemetryClient.trackStatsbeatAsync(requestDurationSt);
    }

    if (local.retryCount.get() != 0) {
      TelemetryItem retryCountSt =
          createStatsbeatTelemetry(
              telemetryClient, RETRY_COUNT_METRIC_NAME, local.retryCount.get());
      addEndpointAndHostToProperties(retryCountSt, host);
      telemetryClient.trackStatsbeatAsync(retryCountSt);
    }

    if (local.throttlingCount.get() != 0) {
      TelemetryItem throttleCountSt =
          createStatsbeatTelemetry(
              telemetryClient, THROTTLE_COUNT_METRIC_NAME, local.throttlingCount.get());
      addEndpointAndHostToProperties(throttleCountSt, host);
      telemetryClient.trackStatsbeatAsync(throttleCountSt);
    }

    if (local.exceptionCount.get() != 0) {
      TelemetryItem exceptionCountSt =
          createStatsbeatTelemetry(
              telemetryClient, EXCEPTION_COUNT_METRIC_NAME, local.exceptionCount.get());
      addEndpointAndHostToProperties(exceptionCountSt, host);
      telemetryClient.trackStatsbeatAsync(exceptionCountSt);
    }
  }

  private static void addEndpointAndHostToProperties(TelemetryItem telemetryItem, String host) {
    Map<String, String> properties =
        TelemetryUtil.getProperties(telemetryItem.getData().getBaseData());
    properties.put("endpoint", BREEZE_ENDPOINT);
    properties.put("host", host);
  }

  private static class IntervalMetrics {
    private final AtomicLong requestSuccessCount = new AtomicLong();
    private final AtomicLong requestFailureCount = new AtomicLong();
    // request duration count only counts request success.
    private final AtomicLong totalRequestDuration = new AtomicLong(); // duration in milliseconds
    private final AtomicLong retryCount = new AtomicLong();
    private final AtomicLong throttlingCount = new AtomicLong();
    private final AtomicLong exceptionCount = new AtomicLong();

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
   * return 'westus-0.in.applicationinsights.azure.com'
   */
  private static String getHost(String endpointUrl) {
    assert (endpointUrl != null && !endpointUrl.isEmpty());
    return endpointUrl.replaceAll("^\\w+://", "").replaceAll("/\\w+.?\\w?/\\w+", "");
  }

  void sendOriginalEndpointCounterOnRedirect(
      TelemetryClient telemetryClient, String ikey, String originalUrl) {
    sendIntervalMetric(telemetryClient, instrumentationKeyCounterMap.get(ikey), originalUrl);
  }
}
