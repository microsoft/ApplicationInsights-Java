package com.microsoft.applicationinsights.agent.internal.localstorage;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryPipelineListener;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

class LocalStorageTelemetryPipelineListener2 implements TelemetryPipelineListener {

  private final LocalFileLoader localFileLoader;
  private final File file;

  LocalStorageTelemetryPipelineListener2(LocalFileLoader localFileLoader, File file) {
    this.localFileLoader = localFileLoader;
    this.file = file;
  }

  @Override
  public void onResponse(
      int responseCode,
      String responseBody,
      List<ByteBuffer> requestBody,
      String instrumentationKey) {
    if (responseCode == 200) {
      localFileLoader.updateProcessedFileStatus(true, file);
    } else {
      localFileLoader.updateProcessedFileStatus(
          !LocalStorageTelemetryPipelineListener.RETRYABLE_CODES.contains(responseCode), file);
    }
  }

  @Override
  public void onError(
      String reason, Throwable error, List<ByteBuffer> requestBody, String instrumentationKey) {
    localFileLoader.updateProcessedFileStatus(false, file);
  }
}
