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

package com.microsoft.applicationinsights.agent.internal.quickpulse;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.microsoft.applicationinsights.agent.internal.common.HostName;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.common.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public enum QuickPulse {
  INSTANCE;

  public static final int QP_INVARIANT_VERSION = 1;
  private volatile boolean initialized = false;

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
        .execute(() -> initializeSync(latch, telemetryClient));
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
              LazyHttpClient.newHttpPipeLine(telemetryClient.getAadAuthentication(), null);
          ArrayBlockingQueue<HttpRequest> sendQueue = new ArrayBlockingQueue<>(256, true);

          QuickPulseDataSender quickPulseDataSender =
              new QuickPulseDataSender(httpPipeline, sendQueue);

          String instanceName = telemetryClient.getRoleInstance();
          String machineName = HostName.get();

          if (Strings.isNullOrEmpty(instanceName)) {
            instanceName = machineName;
          }
          if (Strings.isNullOrEmpty(instanceName)) {
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

          QuickPulseCoordinator coordinator = new QuickPulseCoordinator(coordinatorInitData);

          Thread senderThread =
              new Thread(quickPulseDataSender, QuickPulseDataSender.class.getSimpleName());
          senderThread.setDaemon(true);
          senderThread.start();

          Thread thread = new Thread(coordinator, QuickPulseCoordinator.class.getSimpleName());
          thread.setDaemon(true);
          thread.start();

          QuickPulseDataCollector.INSTANCE.enable(telemetryClient);
        }
      }
    }
  }
}
