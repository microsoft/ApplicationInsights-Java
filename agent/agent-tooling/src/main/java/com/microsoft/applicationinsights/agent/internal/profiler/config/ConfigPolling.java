// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.microsoft.applicationinsights.agent.internal.profiler.ProfilerConfigurationHandler;
import com.microsoft.applicationinsights.agent.internal.profiler.client.ServiceProfilerClient;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Monitors the Service Profiler endpoint for changes to configuration. */
public class ConfigPolling {

  private static final Logger logger = LoggerFactory.getLogger(ConfigPolling.class);

  public static void startPollingForConfigUpdates(
      ScheduledExecutorService scheduledExecutorService,
      ServiceProfilerClient serviceProfilerClient,
      List<ProfilerConfigurationHandler> handlers,
      int pollPeriodSeconds) {

    ConfigService configService = new ConfigService(serviceProfilerClient);
    scheduledExecutorService.scheduleAtFixedRate(
        () -> pollForConfigUpdates(configService, handlers),
        5,
        pollPeriodSeconds,
        TimeUnit.SECONDS);
  }

  private static void pollForConfigUpdates(
      ConfigService configService, List<ProfilerConfigurationHandler> handlers) {
    try {
      configService
          .pullSettings()
          .doOnError(e -> logger.error("Error pulling service profiler settings", e))
          .subscribe(
              newConfig -> handlers.forEach(observer -> observer.updateConfiguration(newConfig)));
    } catch (Throwable t) {
      logger.error("Error pulling service profiler settings", t);
    }
  }

  private ConfigPolling() {}
}
