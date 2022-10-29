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
public class ProfilerInitialization {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilerInitialization.class);

  private final Configuration.ProfilerConfiguration config;
  private final ServiceProfilerClient serviceProfilerClient;
  private final UploadService uploadService;

  @SuppressWarnings("unused")
  private final Profiler profiler;

  private final ScheduledExecutorService serviceProfilerExecutorService;
  private final ProfilerConfigurationHandler profilerConfigurationHandler;

  private final AtomicBoolean initialised = new AtomicBoolean();

  public ProfilerInitialization(
      Configuration.ProfilerConfiguration config,
      Profiler profiler,
      ProfilerConfigurationHandler profilerConfigurationHandler,
      ServiceProfilerClient serviceProfilerClient,
      UploadService uploadService,
      ScheduledExecutorService serviceProfilerExecutorService) {

    this.config = config;
    this.profiler = profiler;
    this.serviceProfilerClient = serviceProfilerClient;
    this.uploadService = uploadService;
    this.serviceProfilerExecutorService = serviceProfilerExecutorService;
    this.profilerConfigurationHandler = profilerConfigurationHandler;
  }

  public Future<ProfilerInitialization> initialize() {
    CompletableFuture<ProfilerInitialization> result = new CompletableFuture<>();
    if (initialised.getAndSet(true)) {
      result.complete(this);
      return result;
    }

    LOGGER.warn("INITIALISING JFR PROFILING SUBSYSTEM THIS FEATURE IS IN BETA");

    serviceProfilerExecutorService.submit(
        () -> {
          try {
            // Daemon remains alive permanently due to scheduling an update
            profiler.initialize(uploadService, serviceProfilerExecutorService);

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

  public Profiler getProfiler() {
    return profiler;
  }
}
