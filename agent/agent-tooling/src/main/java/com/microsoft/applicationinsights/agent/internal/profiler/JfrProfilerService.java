// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.client.ServiceProfilerClient;
import com.microsoft.applicationinsights.agent.internal.profiler.config.ConfigMonitoringService;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadService;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JFR Service Profiler main entry point, wires up the items below.
 *
 * <ul>
 *   <li>Configuration polling
 *   <li>Notifying upstream
 *   <li>consumers (such as the alerting subsystem) of configuration updates
 *   <li>JFR Profiling service
 *   <li>JFR Uploader service
 * </ul>
 */
public class JfrProfilerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JfrProfilerService.class);

  private static final String APP_ID_PREFIX = "cid-v1:";

  private final Configuration.ProfilerConfiguration config;
  private final ServiceProfilerClient serviceProfilerClient;
  private final UploadService uploadService;

  private final Supplier<String> appIdSupplier;

  @SuppressWarnings("unused")
  private final JfrProfiler profiler;

  private final ScheduledExecutorService serviceProfilerExecutorService;
  private final ProfilerConfigurationHandler profilerConfigurationHandler;

  private final AtomicBoolean initialised = new AtomicBoolean();

  public JfrProfilerService(
      Supplier<String> appIdSupplier,
      Configuration.ProfilerConfiguration config,
      JfrProfiler profiler,
      ProfilerConfigurationHandler profilerConfigurationHandler,
      ServiceProfilerClient serviceProfilerClient,
      UploadService uploadService,
      ScheduledExecutorService serviceProfilerExecutorService) {
    this.appIdSupplier = getAppId(appIdSupplier);
    this.config = config;
    this.profiler = profiler;
    this.serviceProfilerClient = serviceProfilerClient;
    this.uploadService = uploadService;
    this.serviceProfilerExecutorService = serviceProfilerExecutorService;
    this.profilerConfigurationHandler = profilerConfigurationHandler;
  }

  public Future<JfrProfilerService> initialize() {
    CompletableFuture<JfrProfilerService> result = new CompletableFuture<>();
    if (initialised.getAndSet(true)) {
      result.complete(this);
      return result;
    }

    LOGGER.warn("INITIALISING JFR PROFILING SUBSYSTEM THIS FEATURE IS IN BETA");

    JfrUploadService jfrUploadService = new JfrUploadService(uploadService, appIdSupplier);

    serviceProfilerExecutorService.submit(
        () -> {
          try {
            // Daemon remains alive permanently due to scheduling an update
            profiler.initialize(jfrUploadService, serviceProfilerExecutorService);

            // Monitor service remains alive permanently due to scheduling an periodic config pull
            ConfigMonitoringService.createServiceProfilerConfigService(
                serviceProfilerExecutorService,
                serviceProfilerClient,
                Arrays.asList(profilerConfigurationHandler, profiler),
                config);

            result.complete(this);
          } catch (Throwable t) {
            LOGGER.error(
                "Failed to initialise profiler service",
                new RuntimeException(
                    "Unable to obtain JFR connection, this may indicate that your JVM does not"
                        + " have JFR enabled. JFR profiling system will shutdown"));
            result.completeExceptionally(t);
          }
        });
    return result;
  }

  private static Supplier<String> getAppId(Supplier<String> supplier) {
    return () -> {
      String appId = supplier.get();

      if (appId == null || appId.isEmpty()) {
        return null;
      }

      if (appId.startsWith(APP_ID_PREFIX)) {
        appId = appId.substring(APP_ID_PREFIX.length());
      }
      return appId;
    };
  }

  public JfrProfiler getProfiler() {
    return profiler;
  }
}
