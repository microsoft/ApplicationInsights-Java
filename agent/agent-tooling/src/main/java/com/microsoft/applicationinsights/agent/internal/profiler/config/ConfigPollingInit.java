// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Monitors the Service Profiler endpoint for changes to configuration. */
public class ConfigPollingInit {

  public static void startPollingForConfigUpdates(
      ScheduledExecutorService scheduledExecutorService,
      ServiceProfilerClient serviceProfilerClient,
      Consumer<ProfilerConfiguration> updateListener,
      int pollPeriodSeconds) {

    ConfigService configService = new ConfigService(serviceProfilerClient);
    scheduledExecutorService.scheduleAtFixedRate(
        () -> configService.pollForConfigUpdates(updateListener),
        5,
        pollPeriodSeconds,
        TimeUnit.SECONDS);
  }

  private ConfigPollingInit() {}
}
