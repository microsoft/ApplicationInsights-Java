// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.microsoft.applicationinsights.agent.internal.profiler.service.ServiceProfilerClient;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Monitors the Service Profiler endpoint for changes to configuration. */
public class ConfigPollingInit {

  public static void startPollingForConfigUpdates(
      ScheduledExecutorService scheduledExecutorService,
      ServiceProfilerClient serviceProfilerClient,
      List<ProfilerConfigurationHandler> handlers,
      int pollPeriodSeconds) {

    ConfigService configService = new ConfigService(serviceProfilerClient);
    scheduledExecutorService.scheduleAtFixedRate(
        () -> configService.pollForConfigUpdates(handlers), 5, pollPeriodSeconds, TimeUnit.SECONDS);
  }

  private ConfigPollingInit() {}
}
