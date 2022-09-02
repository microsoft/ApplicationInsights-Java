// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.profiler;

import com.azure.core.http.HttpPipeline;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

public interface ProfilerServiceFactory {

  @SuppressWarnings("TooManyParameters")
  Future<ProfilerService> initialize(
      Supplier<String> appIdSupplier,
      ProfilerConfigurationHandler profilerConfigurationHandler,
      String processId,
      ServiceProfilerServiceConfig config,
      String machineName,
      String instrumentationKey,
      HttpPipeline httpPipeline,
      ScheduledExecutorService serviceProfilerExecutorService,
      String userAgent,
      String roleName);
}
