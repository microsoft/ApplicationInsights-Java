package com.microsoft.applicationinsights.agent.internal.localstorage;

import static java.util.Arrays.asList;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryPipelineListener;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocalStorageTelemetryPipelineListener implements TelemetryPipelineListener {

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

  public LocalStorageTelemetryPipelineListener(LocalFileWriter localFileWriter) {
    this.localFileWriter = localFileWriter;
  }

  @Override
  public void onResponse(
      int responseCode,
      String responseBody,
      List<ByteBuffer> requestBody,
      String instrumentationKey) {
    if (RETRYABLE_CODES.contains(responseCode)) {
      localFileWriter.writeToDisk(requestBody, instrumentationKey);
    }
  }

  @Override
  public void onError(
      String reason, Throwable error, List<ByteBuffer> requestBody, String instrumentationKey) {
    localFileWriter.writeToDisk(requestBody, instrumentationKey);
  }
}
