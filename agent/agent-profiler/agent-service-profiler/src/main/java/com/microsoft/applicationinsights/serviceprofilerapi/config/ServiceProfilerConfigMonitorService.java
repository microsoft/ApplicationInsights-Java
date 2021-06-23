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

package com.microsoft.applicationinsights.serviceprofilerapi.config;

import com.microsoft.applicationinsights.profiler.ProfilerConfigurationHandler;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Monitors the Service Profiler endpoint for changes to configuration */
public class ServiceProfilerConfigMonitorService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ServiceProfilerConfigMonitorService.class);

  // Execution context for the monitoring
  private final ScheduledExecutorService serviceProfilerExecutorService;

  // period of the polling interval
  private final int pollPeriodInMs;
  private ScheduledFuture<?> future;

  private final ServiceProfilerClientV2 serviceProfilerClient;
  private ServiceProfilerSettingsClient serviceProfilerSettingsClient;

  public static ServiceProfilerConfigMonitorService createServiceProfilerConfigService(
      ScheduledExecutorService serviceProfilerExecutorService,
      ServiceProfilerClientV2 serviceProfilerClient,
      List<ProfilerConfigurationHandler> configObservers,
      ServiceProfilerServiceConfig config) {
    ServiceProfilerConfigMonitorService serviceProfilerConfigMonitorService =
        new ServiceProfilerConfigMonitorService(
            serviceProfilerExecutorService, config.getConfigPollPeriod(), serviceProfilerClient);
    serviceProfilerConfigMonitorService.initialize(configObservers);
    return serviceProfilerConfigMonitorService;
  }

  public ServiceProfilerConfigMonitorService(
      ScheduledExecutorService serviceProfilerExecutorService,
      int pollPeriodInMs,
      ServiceProfilerClientV2 serviceProfilerClient) {

    this.serviceProfilerExecutorService = serviceProfilerExecutorService;
    this.pollPeriodInMs = pollPeriodInMs;
    this.serviceProfilerClient = serviceProfilerClient;
  }

  public void initialize(List<ProfilerConfigurationHandler> observers) {
    scheduleSettingsPull(
        newConfig -> observers.forEach(observer -> observer.updateConfiguration(newConfig)));
  }

  // synchronized to ensure future is not created twice
  private synchronized void scheduleSettingsPull(ProfilerConfigurationHandler handleSettings) {
    if (future != null) {
      throw new IllegalStateException("Service already initialized");
    }

    serviceProfilerSettingsClient = new ServiceProfilerSettingsClient(serviceProfilerClient);

    // schedule regular config pull
    future =
        serviceProfilerExecutorService.scheduleAtFixedRate(
            pull(handleSettings), 0, pollPeriodInMs, TimeUnit.MILLISECONDS);
  }

  // pull settings from the endpoint
  private Runnable pull(ProfilerConfigurationHandler handleSettings) {
    return () -> {
      try {
        serviceProfilerSettingsClient
            .pullSettings()
            .doOnError(
                e -> {
                  LOGGER.error("Error pulling service profiler settings", e);
                })
            .subscribe(handleSettings::updateConfiguration);
      } catch (RuntimeException e) {
        LOGGER.error("Error pulling service profiler settings", e);
      } catch (Error e) {
        LOGGER.error("Error pulling service profiler settings", e);
        throw e;
      }
    };
  }
}
