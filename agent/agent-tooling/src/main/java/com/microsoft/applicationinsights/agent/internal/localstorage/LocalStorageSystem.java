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

package com.microsoft.applicationinsights.agent.internal.localstorage;

import com.microsoft.applicationinsights.agent.internal.statsbeat.NonessentialStatsbeat;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryByteBufferPipeline;
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

  public void startSendingFromDisk(TelemetryByteBufferPipeline channel) {
    LocalFileSender.start(loader, channel);
  }

  public void stop() {
    // TODO (trask) this will be needed in azure-monitor-opentelemetry-exporter
  }
}
