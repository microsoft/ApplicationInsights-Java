// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.microsoft.applicationinsights.agent.internal.profiler.client.ServiceProfilerClient;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ConfigMonitoringService;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ProfilerConfiguration;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class ConfigMonitoringServiceTest {

  @Test
  void pullsConfig() throws IOException, URISyntaxException {

    AtomicReference<Runnable> job = new AtomicReference<>();

    ScheduledExecutorService executorService = mockScheduledExecutorService(job::set);
    ServiceProfilerClient serviceProfilerClient = mockServiceProfilerClient();

    ConfigMonitoringService serviceMonitor =
        new ConfigMonitoringService(executorService, 100, serviceProfilerClient);

    AtomicReference<ProfilerConfiguration> config = new AtomicReference<>();
    serviceMonitor.initialize(Collections.singletonList(config::set));

    assertThat(job.get()).isNotNull();

    job.get().run();

    Mockito.verify(serviceProfilerClient, times(1)).getSettings(any(Date.class));
    assertThat(config.get()).isNotNull();

    assertThat(config.get().getCollectionPlan())
        .isEqualTo(
            "--single --mode immediate --immediate-profiling-duration 120  --expiration 5249157885138288517 --settings-moniker a-settings-moniker");
    assertThat(config.get().getCpuTriggerConfiguration())
        .isEqualTo(
            "--cpu-trigger-enabled true --cpu-threshold 80 --cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400");
    assertThat(config.get().getDefaultConfiguration())
        .isEqualTo("--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120");
    assertThat(config.get().getMemoryTriggerConfiguration())
        .isEqualTo(
            "--memory-trigger-enabled true --memory-threshold 20 --memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400");
  }

  private static ScheduledExecutorService mockScheduledExecutorService(Consumer<Runnable> job) {
    ScheduledExecutorService executorService = Mockito.mock(ScheduledExecutorService.class);
    when(executorService.scheduleAtFixedRate(
            any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
        .thenAnswer(
            invocation -> {
              job.accept(invocation.getArgument(0, Runnable.class));
              return null;
            });
    return executorService;
  }

  static ServiceProfilerClient mockServiceProfilerClient() throws IOException, URISyntaxException {
    ServiceProfilerClient serviceProfilerClient = Mockito.mock(ServiceProfilerClient.class);
    when(serviceProfilerClient.getSettings(any(Date.class)))
        .thenReturn(
            Mono.just(
                "{\"id\":\"8929ed2e-24da-4ad4-8a8b-5a5ebc03abb4\",\"lastModified\":\"2021-01-25T15:46:11"
                    + ".0900613+00:00\",\"enabledLastModified\":\"0001-01-01T00:00:00+00:00\",\"enabled\":true,\"collectionPlan\":\"--single --mode immediate --immediate-profiling-duration 120  "
                    + "--expiration 5249157885138288517 --settings-moniker a-settings-moniker\",\"cpuTriggerConfiguration\":\"--cpu-trigger-enabled true --cpu-threshold 80 "
                    + "--cpu-trigger-profilingDuration 30 --cpu-trigger-cooldown 14400\",\"memoryTriggerConfiguration\":\"--memory-trigger-enabled true --memory-threshold 20 "
                    + "--memory-trigger-profilingDuration 120 --memory-trigger-cooldown 14400\",\"defaultConfiguration\":\"--sampling-enabled true --sampling-rate 5 --sampling-profiling-duration 120\","
                    + "\"geoOverride\":null}"));
    return serviceProfilerClient;
  }
}
