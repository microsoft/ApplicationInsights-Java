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

package com.microsoft.applicationinsights.internal.quickpulse;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.channel.common.LazyHttpClient;
import com.microsoft.applicationinsights.internal.util.DeviceInfo;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public enum QuickPulse {
  INSTANCE;

  public static final int QP_INVARIANT_VERSION = 1;
  private volatile boolean initialized = false;
  private Thread thread;
  private Thread senderThread;
  private QuickPulseCoordinator coordinator;
  private QuickPulseDataSender quickPulseDataSender;

  // initialization is performed in the background because initializing the random seed (via
  // UUID.randomUUID()) below
  // can cause slowness during startup in some environments
  @Deprecated
  public void initialize() {
    initialize(TelemetryClient.getActive());
  }

  public void initialize(TelemetryClient telemetryClient) {
    CountDownLatch latch = new CountDownLatch(1);
    Executors.newSingleThreadExecutor(ThreadPoolUtils.createDaemonThreadFactory(QuickPulse.class))
        .execute(
            new Runnable() {
              @Override
              public void run() {
                initializeSync(latch, telemetryClient);
              }
            });
    // don't return until initialization thread has INSTANCE lock
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void initializeSync(CountDownLatch latch, TelemetryClient telemetryClient) {
    if (initialized) {
      latch.countDown();
    } else {
      synchronized (INSTANCE) {
        latch.countDown();
        if (!initialized) {
          initialized = true;
          String quickPulseId = UUID.randomUUID().toString().replace("-", "");
          HttpPipeline httpPipeline =
              LazyHttpClient.newHttpPipeLine(telemetryClient.getAadAuthentication());
          ArrayBlockingQueue<HttpRequest> sendQueue = new ArrayBlockingQueue<>(256, true);

          quickPulseDataSender = new QuickPulseDataSender(httpPipeline, sendQueue);

          String instanceName = telemetryClient.getRoleInstance();
          String machineName = DeviceInfo.getHostName();

          if (LocalStringsUtils.isNullOrEmpty(instanceName)) {
            instanceName = machineName;
          }
          if (LocalStringsUtils.isNullOrEmpty(instanceName)) {
            instanceName = "Unknown host";
          }

          QuickPulsePingSender quickPulsePingSender =
              new QuickPulsePingSender(
                  httpPipeline, telemetryClient, machineName, instanceName, quickPulseId);
          QuickPulseDataFetcher quickPulseDataFetcher =
              new QuickPulseDataFetcher(
                  sendQueue, telemetryClient, machineName, instanceName, quickPulseId);

          QuickPulseCoordinatorInitData coordinatorInitData =
              new QuickPulseCoordinatorInitDataBuilder()
                  .withPingSender(quickPulsePingSender)
                  .withDataFetcher(quickPulseDataFetcher)
                  .withDataSender(quickPulseDataSender)
                  .build();

          coordinator = new QuickPulseCoordinator(coordinatorInitData);

          senderThread =
              new Thread(quickPulseDataSender, QuickPulseDataSender.class.getSimpleName());
          senderThread.setDaemon(true);
          senderThread.start();

          thread = new Thread(coordinator, QuickPulseCoordinator.class.getSimpleName());
          thread.setDaemon(true);
          thread.start();

          QuickPulseDataCollector.INSTANCE.enable(telemetryClient);
        }
      }
    }
  }

  /**
   * Stopping the collection of performance data.
   *
   * @param timeout The timeout to wait for the stop to happen.
   * @param timeUnit The time unit to use when waiting for the stop to happen.
   */
  public synchronized void stop(long timeout, TimeUnit timeUnit) {
    if (!initialized) {
      return;
    }

    coordinator.stop();
    quickPulseDataSender.stop();

    thread.interrupt();
    senderThread.interrupt();
    initialized = false;

    try {
      thread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    try {
      senderThread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
