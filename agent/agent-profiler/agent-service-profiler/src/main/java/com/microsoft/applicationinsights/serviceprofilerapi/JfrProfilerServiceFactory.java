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

package com.microsoft.applicationinsights.serviceprofilerapi;

import com.azure.core.http.HttpPipeline;
import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.profiler.ProfilerConfigurationHandler;
import com.microsoft.applicationinsights.profiler.ProfilerService;
import com.microsoft.applicationinsights.profiler.ProfilerServiceFactory;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import com.microsoft.applicationinsights.profiler.uploader.UploadCompleteHandler;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ProfilerFrontendClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.profiler.JfrProfiler;
import com.microsoft.applicationinsights.serviceprofilerapi.upload.ServiceProfilerUploader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Default ProfilerService factory loaded by a service loader, produces a Profiler Service based on
 * JFR.
 */
@AutoService(ProfilerServiceFactory.class)
public class JfrProfilerServiceFactory implements ProfilerServiceFactory {
  // Singleton instance that holds the one and only service of the ServiceProfiler subsystem
  private static JfrProfilerService instance;

  @Override
  public synchronized Future<ProfilerService> initialize(
      Supplier<String> appIdSupplier,
      UploadCompleteHandler uploadCompleteObserver,
      ProfilerConfigurationHandler profilerConfigurationHandler,
      String processId,
      ServiceProfilerServiceConfig config,
      String machineName,
      String instrumentationKey,
      HttpPipeline httpPipeline,
      ScheduledExecutorService serviceProfilerExecutorService,
      String userAgent,
      String roleName) {
    if (instance == null) {
      ServiceProfilerClientV2 serviceProfilerClient =
          new ProfilerFrontendClientV2(
              config.getServiceProfilerFrontEndPoint(),
              instrumentationKey,
              httpPipeline,
              userAgent);

      ServiceProfilerUploader uploader =
          new ServiceProfilerUploader(
              serviceProfilerClient, machineName, processId, appIdSupplier, roleName);

      instance =
          new JfrProfilerService(
              appIdSupplier,
              config,
              new JfrProfiler(config),
              profilerConfigurationHandler,
              uploadCompleteObserver,
              serviceProfilerClient,
              uploader,
              serviceProfilerExecutorService);

      return instance.initialize();
    }
    return CompletableFuture.completedFuture(instance);
  }
}
