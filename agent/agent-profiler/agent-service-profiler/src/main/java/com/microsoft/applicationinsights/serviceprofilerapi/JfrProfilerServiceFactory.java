// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi;

import com.azure.core.http.HttpPipeline;
import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.profiler.ProfilerConfigurationHandler;
import com.microsoft.applicationinsights.profiler.ProfilerService;
import com.microsoft.applicationinsights.profiler.ProfilerServiceFactory;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClient;
import com.microsoft.applicationinsights.serviceprofilerapi.profiler.JfrProfiler;
import com.microsoft.applicationinsights.serviceprofilerapi.upload.ServiceProfilerUploader;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Default ProfilerService factory loaded by a service loader, produces a Profiler Service based on
 * JFR.
 */
@AutoService(ProfilerServiceFactory.class)
public class JfrProfilerServiceFactory implements ProfilerServiceFactory {

  @Override
  public synchronized Future<ProfilerService> initialize(
      Supplier<String> appIdSupplier,
      ProfilerConfigurationHandler profilerConfigurationHandler,
      String processId,
      ServiceProfilerServiceConfig config,
      String machineName,
      String instrumentationKey,
      HttpPipeline httpPipeline,
      ScheduledExecutorService serviceProfilerExecutorService,
      String userAgent,
      String roleName) {
    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(
            config.getServiceProfilerFrontEndPoint(), instrumentationKey, httpPipeline, userAgent);

    ServiceProfilerUploader uploader =
        new ServiceProfilerUploader(
            serviceProfilerClient, machineName, processId, appIdSupplier, roleName);

    JfrProfilerService instance =
        new JfrProfilerService(
            appIdSupplier,
            config,
            new JfrProfiler(config),
            profilerConfigurationHandler,
            serviceProfilerClient,
            uploader,
            serviceProfilerExecutorService);

    return instance.initialize();
  }
}
