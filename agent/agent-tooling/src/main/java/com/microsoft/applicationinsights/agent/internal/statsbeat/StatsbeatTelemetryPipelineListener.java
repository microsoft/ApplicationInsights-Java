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

import static java.util.Arrays.asList;

import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineRequest;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineResponse;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class StatsbeatTelemetryPipelineListener implements TelemetryPipelineListener {

  // not including 401/403/503 in this list because those are commonly returned by proxy servers
  // when they are not configured to allow traffic for westus-0
  // not including 307/308 in this list because redirects only bubble up to this class if they have
  // reached the 10 redirect threshold, in which case they are considered non-retryable exceptions
  private static final Set<Integer> RESPONSE_CODES_INDICATING_REACHED_BREEZE =
      new HashSet<>(asList(200, 206, 402, 408, 429, 439, 500));

  private final Runnable statsbeatShutdown;

  private final AtomicInteger statsbeatUnableToReachBreezeCounter = new AtomicInteger();

  private volatile boolean statsbeatHasReachedBreezeAtLeastOnce;

  public StatsbeatTelemetryPipelineListener(Runnable statsbeatShutdown) {
    this.statsbeatShutdown = statsbeatShutdown;
  }

  @Override
  public void onResponse(TelemetryPipelineRequest request, TelemetryPipelineResponse response) {
    int statusCode = response.getStatusCode();
    if (!statsbeatHasReachedBreezeAtLeastOnce) {
      if (RESPONSE_CODES_INDICATING_REACHED_BREEZE.contains(statusCode)) {
        statsbeatHasReachedBreezeAtLeastOnce = true;
      } else {
        statsbeatDidNotReachBreeze();
      }
    }
  }

  @Override
  public void onException(
      TelemetryPipelineRequest request, String errorMessage, Throwable throwable) {
    if (!statsbeatHasReachedBreezeAtLeastOnce) {
      statsbeatDidNotReachBreeze();
    }
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  private void statsbeatDidNotReachBreeze() {
    if (statsbeatUnableToReachBreezeCounter.getAndIncrement() >= 10) {
      // shutting down statsbeat because it's unlikely that it will ever get through at this point
      // some possible reasons:
      // * AMPLS
      // * proxy that has not been configured to allow westus-0
      // * local firewall that has not been configured to allow westus-0
      //
      // TODO need to figure out a way that statsbeat telemetry can be sent to the same endpoint as
      // the customer data for these cases
      statsbeatShutdown.run();
    }
  }
}
