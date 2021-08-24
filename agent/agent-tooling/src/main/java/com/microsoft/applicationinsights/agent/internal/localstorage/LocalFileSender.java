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

import com.microsoft.applicationinsights.agent.internal.common.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryChannel;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import io.opentelemetry.sdk.common.CompletableResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalFileSender implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(LocalFileSender.class);

  // send persisted telemetries from local disk every 30 seconds.
  private static final long INTERVAL_SECONDS = 30;
  private static final ScheduledExecutorService scheduledExecutor =
      Executors.newSingleThreadScheduledExecutor(
          ThreadPoolUtils.createDaemonThreadFactory(LocalFileLoader.class));

  private final LocalFileLoader localFileLoader;
  private final TelemetryChannel telemetryChannel;

  public static void start(LocalFileLoader localFileLoader, TelemetryChannel telemetryChannel) {
    LocalFileSender localFileSender = new LocalFileSender(localFileLoader, telemetryChannel);
    scheduledExecutor.scheduleWithFixedDelay(
        localFileSender, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  private LocalFileSender(LocalFileLoader localFileLoader, TelemetryChannel telemetryChannel) {
    this.localFileLoader = localFileLoader;
    this.telemetryChannel = telemetryChannel;
  }

  @Override
  public void run() {
    try {
      ByteBuffer buffer = localFileLoader.loadTelemetriesFromDisk();
      if (buffer != null) {
        CompletableResultCode resultCode = telemetryChannel.sendRawBytes(buffer);
        localFileLoader.updateProcessedFileStatus(resultCode.isSuccess());
      }
    } catch (RuntimeException ex) {
      logger.error(
          "Unexpected error occurred while sending telemetries from the local storage.", ex);
      // TODO (heya) track sending persisted telemetries failure via Statsbeat.
    }
  }
}
