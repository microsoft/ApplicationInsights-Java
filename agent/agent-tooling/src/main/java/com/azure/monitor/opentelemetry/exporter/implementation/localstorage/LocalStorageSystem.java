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

package com.azure.monitor.opentelemetry.exporter.implementation.localstorage;

import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipeline;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineListener;
import java.io.File;

public class LocalStorageSystem {

  private final File telemetryFolder;
  private final LocalFileWriter writer;
  private final LocalFileLoader loader;

  public LocalStorageSystem(File telemetryFolder, LocalStorageStats stats) {
    this.telemetryFolder = telemetryFolder;
    LocalFileCache localFileCache = new LocalFileCache(telemetryFolder);
    loader = new LocalFileLoader(localFileCache, telemetryFolder, stats);
    writer = new LocalFileWriter(localFileCache, telemetryFolder, stats);
  }

  public TelemetryPipelineListener createTelemetryPipelineListener() {
    return new LocalStorageTelemetryPipelineListener(writer);
  }

  public void startSendingFromDisk(TelemetryPipeline pipeline) {
    LocalFileSender.start(loader, pipeline);
    LocalFilePurger.startPurging(telemetryFolder);
  }

  public void stop() {
    // TODO (trask) this will be needed in azure-monitor-opentelemetry-exporter
  }
}
