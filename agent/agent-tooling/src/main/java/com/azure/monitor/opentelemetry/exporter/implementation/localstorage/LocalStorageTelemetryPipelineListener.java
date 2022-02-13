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

import static java.util.Arrays.asList;

import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipeline;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineListener;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineRequest;
import com.azure.monitor.opentelemetry.exporter.implementation.pipeline.TelemetryPipelineResponse;
import java.io.File;
import java.util.HashSet;
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
  private final LocalFileSender localFileSender;
  private final LocalFilePurger localFilePurger;

  // telemetryFolder must already exist and be writable
  public LocalStorageTelemetryPipelineListener(
      File telemetryFolder, TelemetryPipeline pipeline, LocalStorageStats stats) {

    LocalFileCache localFileCache = new LocalFileCache(telemetryFolder);
    LocalFileLoader loader = new LocalFileLoader(localFileCache, telemetryFolder, stats);
    localFileWriter = new LocalFileWriter(localFileCache, telemetryFolder, stats);

    localFileSender = new LocalFileSender(loader, pipeline);
    localFilePurger = new LocalFilePurger(telemetryFolder);
  }

  public void shutdown() {
    localFileSender.shutdown();
    localFilePurger.shutdown();
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
