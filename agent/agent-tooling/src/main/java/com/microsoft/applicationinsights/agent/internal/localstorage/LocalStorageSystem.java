package com.microsoft.applicationinsights.agent.internal.localstorage;

import com.microsoft.applicationinsights.agent.internal.statsbeat.NonessentialStatsbeat;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryPipeline;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryPipelineListener;
import java.io.File;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LocalStorageSystem {

  private final LocalFileWriter writer;
  private final LocalFileLoader loader;

  public LocalStorageSystem(
      File telemetryFolder, @Nullable NonessentialStatsbeat nonessentialStatsbeat) {
    LocalFileCache localFileCache = new LocalFileCache(telemetryFolder);
    loader = new LocalFileLoader(localFileCache, telemetryFolder, nonessentialStatsbeat);
    writer = new LocalFileWriter(localFileCache, telemetryFolder, nonessentialStatsbeat);
  }

  public TelemetryPipelineListener createTelemetryChannelListener() {
    return new LocalStorageTelemetryPipelineListener(writer);
  }

  public void startSendingFromDisk(TelemetryPipeline channel) {
    LocalFileSender.start(loader, channel);
  }

  public void stop() {
    // TODO (trask) this will be needed in azure-monitor-opentelemetry-exporter
  }
}
