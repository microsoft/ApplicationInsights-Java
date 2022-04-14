package com.microsoft.applicationinsights.agent.internal.statsbeat;

import static java.util.Arrays.asList;

import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineRequest;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StatsbeatTelemetryPipelineListener implements TelemetryPipelineListener {

  // not including 401/403/503 in this list because those are commonly returned by proxy servers
  // when they are not configured to allow traffic for westus-0
  // not including 307/308 in this list because redirects only bubble up to this class if they have
  // reached the 10 redirect threshold, in which case they are considered non-retryable exceptions
  private static final Set<Integer> RESPONSE_CODES_INDICATING_REACHED_BREEZE =
      new HashSet<>(asList(200, 206, 402, 408, 429, 439, 500));

  private final StatsbeatModule statsbeatModule;
  private final Runnable localStorageTelemetryShutdownFunction;

  private final AtomicInteger statsbeatUnableToReachBreezeCounter = new AtomicInteger();
  // TODO (trask) remove this boolean and shutdown the disk loader for statsbeat instead
  private final AtomicBoolean statsbeatHasBeenShutdown = new AtomicBoolean();

  private volatile boolean statsbeatHasReachedBreezeAtLeastOnce;

  public StatsbeatTelemetryPipelineListener(
      StatsbeatModule statsbeatModule, Runnable localStorageTelemetryShutdownFunction) {
    this.statsbeatModule = statsbeatModule;
    this.localStorageTelemetryShutdownFunction = localStorageTelemetryShutdownFunction;
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

  private void statsbeatDidNotReachBreeze() {
    if (statsbeatUnableToReachBreezeCounter.getAndIncrement() >= 10
        && !statsbeatHasBeenShutdown.getAndSet(true)) {
      // shutting down statsbeat because it's unlikely that it will ever get through at this point
      // some possible reasons:
      // * AMPLS
      // * proxy that has not been configured to allow westus-0
      // * local firewall that has not been configured to allow westus-0
      //
      // TODO need to figure out a way that statsbeat telemetry can be sent to the same endpoint as
      // the customer data for these cases
      statsbeatModule.shutdown();
      localStorageTelemetryShutdownFunction.run();
    }
  }
}
