package com.microsoft.applicationinsights.agent.internal.localstorage;

import static java.util.Arrays.asList;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryPipelineListener;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryPipelineRequest;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryPipelineResponse;
import java.util.HashSet;
import java.util.Set;

class LocalStorageTelemetryPipelineListener implements TelemetryPipelineListener {

  static final Set<Integer> RETRYABLE_CODES =
      new HashSet<>(
          asList(
              401,
              403,
              408, // REQUEST TIMEOUT
              429, // TOO MANY REQUESTS
              500, // INTERNAL SERVER ERROR
              503 // SERVICE UNAVAILABLE
              ));

  private final LocalFileWriter localFileWriter;

  LocalStorageTelemetryPipelineListener(LocalFileWriter localFileWriter) {
    this.localFileWriter = localFileWriter;
  }

  @Override
  public void onResponse(TelemetryPipelineRequest request, TelemetryPipelineResponse response) {
    if (RETRYABLE_CODES.contains(response.getStatusCode())) {
      localFileWriter.writeToDisk(request.getInstrumentationKey(), request.getTelemetry());
    }
  }

  @Override
  public void onException(
      TelemetryPipelineRequest request, String errorMessage, Throwable throwable) {
    localFileWriter.writeToDisk(request.getInstrumentationKey(), request.getTelemetry());
  }
}
