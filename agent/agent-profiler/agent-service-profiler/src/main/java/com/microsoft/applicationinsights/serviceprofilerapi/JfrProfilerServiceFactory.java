// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi;

import com.azure.core.http.HttpPipeline;
import com.microsoft.applicationinsights.serviceprofilerapi.config.LocalConfig;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClient;
import com.microsoft.applicationinsights.serviceprofilerapi.profiler.JfrProfiler;
import com.microsoft.applicationinsights.serviceprofilerapi.upload.UploadService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

public class JfrProfilerServiceFactory {

  @SuppressWarnings("TooManyParameters")
  public synchronized Future<JfrProfilerService> initialize(
      Supplier<String> appIdSupplier,
      ProfilerConfigurationHandler profilerConfigurationHandler,
      String processId,
      LocalConfig config,
      String machineName,
      String instrumentationKey,
      HttpPipeline httpPipeline,
      ScheduledExecutorService serviceProfilerExecutorService,
      String userAgent,
      String roleName) {

    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(
            config.getServiceProfilerFrontEndPoint(), instrumentationKey, httpPipeline, userAgent);

    UploadService uploader =
        new UploadService(serviceProfilerClient, machineName, processId, appIdSupplier, roleName);

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
