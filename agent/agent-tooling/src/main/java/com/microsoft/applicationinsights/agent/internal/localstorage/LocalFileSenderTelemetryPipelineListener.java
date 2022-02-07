package com.microsoft.applicationinsights.agent.internal.localstorage;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryPipelineListener;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryPipelineRequest;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryPipelineResponse;
import java.io.File;

class LocalFileSenderTelemetryPipelineListener implements TelemetryPipelineListener {

  private final LocalFileLoader localFileLoader;
  private final File file;

  LocalFileSenderTelemetryPipelineListener(LocalFileLoader localFileLoader, File file) {
    this.localFileLoader = localFileLoader;
    this.file = file;
  }

  @Override
  public void onResponse(TelemetryPipelineRequest request, TelemetryPipelineResponse response) {
    int responseCode = response.getStatusCode();
    if (responseCode == 200) {
      localFileLoader.updateProcessedFileStatus(true, file);
    } else {
      localFileLoader.updateProcessedFileStatus(
          !LocalStorageTelemetryPipelineListener.RETRYABLE_CODES.contains(responseCode), file);
    }
  }

  @Override
  public void onException(
      TelemetryPipelineRequest request, String errorMessage, Throwable throwable) {
    localFileLoader.updateProcessedFileStatus(false, file);
  }
}
