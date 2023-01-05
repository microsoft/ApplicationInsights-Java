// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.monitor.opentelemetry.exporter.implementation.quickpulse;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpRequest;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.HostName;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.Strings;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.ThreadPoolUtils;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import reactor.util.annotation.Nullable;

public class QuickPulse {

  static final int QP_INVARIANT_VERSION = 1;

  private volatile QuickPulseDataCollector collector;

  public static QuickPulse create(
      HttpPipeline httpPipeline,
      Supplier<URL> endpointUrl,
      Supplier<String> instrumentationKey,
      @Nullable String roleName,
      @Nullable String roleInstance,
      boolean useNormalizedValueForNonNormalizedCpuPercentage,
      String sdkVersion) {

    QuickPulse quickPulse = new QuickPulse();

    // initialization is delayed and performed in the background because initializing the random
    // seed via UUID.randomUUID() below can cause slowness during startup in some environments
    ExecutorService executor =
        Executors.newSingleThreadExecutor(
            ThreadPoolUtils.createDaemonThreadFactory(QuickPulse.class));
    executor.execute(
        () -> {
          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          quickPulse.initialize(
              httpPipeline,
              endpointUrl,
              instrumentationKey,
              roleName,
              roleInstance,
              useNormalizedValueForNonNormalizedCpuPercentage,
              sdkVersion);
        });
    if (executor.isTerminated()) {
      // this condition will always be false, and only exists to ensure the executor can't become
      // unreachable until after execute() method above completes which could cause the executor
      // to be terminated and cause the above method to throw RejectedExecutionException
      // (see https://bugs.openjdk.org/browse/JDK-8145304)
      throw new AssertionError();
    }
    return quickPulse;
  }

  public boolean isEnabled() {
    return collector.isEnabled();
  }

  public void add(TelemetryItem telemetryItem) {
    if (collector != null) {
      collector.add(telemetryItem);
    }
  }

  private void initialize(
      HttpPipeline httpPipeline,
      Supplier<URL> endpointUrl,
      Supplier<String> instrumentationKey,
      @Nullable String roleName,
      @Nullable String roleInstance,
      boolean useNormalizedValueForNonNormalizedCpuPercentage,
      String sdkVersion) {

    String quickPulseId = UUID.randomUUID().toString().replace("-", "");
    ArrayBlockingQueue<HttpRequest> sendQueue = new ArrayBlockingQueue<>(256, true);

    QuickPulseDataSender quickPulseDataSender = new QuickPulseDataSender(httpPipeline, sendQueue);

    String instanceName = roleInstance;
    String machineName = HostName.get();

    if (Strings.isNullOrEmpty(instanceName)) {
      instanceName = machineName;
    }
    if (Strings.isNullOrEmpty(instanceName)) {
      instanceName = "Unknown host";
    }

    QuickPulseDataCollector collector =
        new QuickPulseDataCollector(useNormalizedValueForNonNormalizedCpuPercentage);

    QuickPulsePingSender quickPulsePingSender =
        new QuickPulsePingSender(
            httpPipeline,
            endpointUrl,
            instrumentationKey,
            roleName,
            instanceName,
            machineName,
            quickPulseId,
            sdkVersion);
    QuickPulseDataFetcher quickPulseDataFetcher =
        new QuickPulseDataFetcher(
            collector,
            sendQueue,
            endpointUrl,
            instrumentationKey,
            roleName,
            instanceName,
            machineName,
            quickPulseId);

    QuickPulseCoordinatorInitData coordinatorInitData =
        new QuickPulseCoordinatorInitDataBuilder()
            .withPingSender(quickPulsePingSender)
            .withDataFetcher(quickPulseDataFetcher)
            .withDataSender(quickPulseDataSender)
            .withCollector(collector)
            .build();

    QuickPulseCoordinator coordinator = new QuickPulseCoordinator(coordinatorInitData);

    Thread senderThread =
        new Thread(quickPulseDataSender, QuickPulseDataSender.class.getSimpleName());
    senderThread.setDaemon(true);
    senderThread.start();

    Thread thread = new Thread(coordinator, QuickPulseCoordinator.class.getSimpleName());
    thread.setDaemon(true);
    thread.start();

    collector.enable(instrumentationKey);

    this.collector = collector;
  }
}
