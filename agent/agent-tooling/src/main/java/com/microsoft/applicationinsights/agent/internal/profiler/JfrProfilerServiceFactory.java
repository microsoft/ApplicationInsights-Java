// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.azure.core.http.HttpPipeline;
import com.microsoft.applicationinsights.agent.internal.common.FriendlyException;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.client.ServiceProfilerClient;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadService;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

public class JfrProfilerServiceFactory {

  @SuppressWarnings("TooManyParameters")
  public synchronized Future<JfrProfilerService> initialize(
      Supplier<String> appIdSupplier,
      ProfilerConfigurationHandler profilerConfigurationHandler,
      String processId,
      Configuration.ProfilerConfiguration config,
      String machineName,
      String instrumentationKey,
      HttpPipeline httpPipeline,
      ScheduledExecutorService serviceProfilerExecutorService,
      String userAgent,
      String roleName,
      File tempDir) {

    ServiceProfilerClient serviceProfilerClient =
        new ServiceProfilerClient(
            getServiceProfilerFrontEndPoint(config), instrumentationKey, httpPipeline, userAgent);

    UploadService uploader =
        new UploadService(serviceProfilerClient, machineName, processId, appIdSupplier, roleName);

    JfrProfilerService instance =
        new JfrProfilerService(
            appIdSupplier,
            config,
            new JfrProfiler(config, tempDir),
            profilerConfigurationHandler,
            serviceProfilerClient,
            uploader,
            serviceProfilerExecutorService);

    return instance.initialize();
  }

  private static URL getServiceProfilerFrontEndPoint(Configuration.ProfilerConfiguration config) {

    // If the user has overridden their service profiler endpoint use that url
    if (config.serviceProfilerFrontEndPoint != null) {
      try {
        return new URL(config.serviceProfilerFrontEndPoint);
      } catch (MalformedURLException e) {
        throw new FriendlyException(
            "Failed to parse url: " + config.serviceProfilerFrontEndPoint,
            "Ensure that the service profiler endpoint is a valid url",
            e);
      }
    }

    return TelemetryClient.getActive().getConnectionString().getProfilerEndpoint();
  }
}
