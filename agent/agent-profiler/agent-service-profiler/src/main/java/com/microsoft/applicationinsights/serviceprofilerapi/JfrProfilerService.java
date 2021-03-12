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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import javax.management.InstanceNotFoundException;

import com.microsoft.applicationinsights.profileUploader.UploadCompleteHandler;
import com.microsoft.applicationinsights.profiler.ProfileHandler;
import com.microsoft.applicationinsights.profiler.Profiler;
import com.microsoft.applicationinsights.profiler.ProfilerConfigurationHandler;
import com.microsoft.applicationinsights.profiler.ProfilerService;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.config.ServiceProfilerConfigMonitorService;
import com.microsoft.applicationinsights.serviceprofilerapi.profiler.JfrUploadService;
import com.microsoft.applicationinsights.serviceprofilerapi.upload.ServiceProfilerUploader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JFR Service Profiler main entry point, wires up:
 * - Configuration polling
 *  - Notifying upstream consumers (such as the alerting subsystem) of configuration updates
 * - JFR Profiling service
 * - JFR Uploader service
 */
public class JfrProfilerService implements ProfilerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JfrProfilerService.class);

    private static final String APP_ID_PREFIX = "cid-v1:";

    private final ServiceProfilerServiceConfig config;
    private final ServiceProfilerClientV2 serviceProfilerClient;
    private final ServiceProfilerUploader serviceProfilerUploader;

    private final Supplier<String> appIdSupplier;

    @SuppressWarnings("unused")
    private final Profiler profiler;
    private final UploadCompleteHandler uploadCompleteObserver;
    private final ScheduledExecutorService serviceProfilerExecutorService;
    private final ProfilerConfigurationHandler profilerConfigurationHandler;

    private boolean initialised = false;

    private ProfileHandler profileHandler;

    public JfrProfilerService(Supplier<String> appIdSupplier,
                              ServiceProfilerServiceConfig config,
                              Profiler profiler,
                              ProfilerConfigurationHandler profilerConfigurationHandler,
                              UploadCompleteHandler uploadCompleteObserver,
                              ServiceProfilerClientV2 serviceProfilerClient,
                              ServiceProfilerUploader serviceProfilerUploader,
                              ScheduledExecutorService serviceProfilerExecutorService
    ) {
        this.appIdSupplier = getAppId(appIdSupplier);
        this.config = config;
        this.profiler = profiler;
        this.serviceProfilerClient = serviceProfilerClient;
        this.serviceProfilerUploader = serviceProfilerUploader;
        this.serviceProfilerExecutorService = serviceProfilerExecutorService;
        this.uploadCompleteObserver = uploadCompleteObserver;
        this.profilerConfigurationHandler = profilerConfigurationHandler;
    }

    public Future<ProfilerService> initialize() {
        CompletableFuture<ProfilerService> result = new CompletableFuture<>();
        if (!config.enabled()) {
            result.completeExceptionally(new IllegalStateException("Profiler disabled"));
            return result;
        }
        if (initialised || !config.enabled()) {
            result.complete(this);
            return result;
        }

        LOGGER.warn("INITIALISING JFR PROFILING SUBSYSTEM THIS FEATURE IS IN BETA");

        initialised = true;

        appIdSupplier.get();

        profileHandler = new JfrUploadService(serviceProfilerUploader, appIdSupplier, uploadCompleteObserver);


        serviceProfilerExecutorService.submit(() -> {
            try {
                if (!initialiseProfiler()) {
                    result.completeExceptionally(
                            new RuntimeException("Unable to obtain JFR connection, this may indicate that your JVM does not have JFR enabled. JFR profiling system will shutdown"));
                    return;
                }

                // Monitor service remains alive permanently due to scheduling an periodic config pull
                ServiceProfilerConfigMonitorService
                        .createServiceProfilerConfigService(
                                serviceProfilerExecutorService,
                                serviceProfilerClient,
                                Arrays.asList(profilerConfigurationHandler, profiler),
                                config);

                result.complete(this);
            } catch (Exception e) {
                LOGGER.error("Failed to initialise alert service", e);
            } catch (Error e) {
                LOGGER.error("Failed to initialise alert service", e);
                throw e;
            }
        });
        return result;
    }

    private boolean initialiseProfiler() {
        boolean initSucceded = false;
        try {
            // Daemon remains alive permanently due to scheduling an update
            initSucceded = profiler.initialize(profileHandler, serviceProfilerExecutorService);
        } catch (IOException | InstanceNotFoundException e) {
            LOGGER.error("Could not initialize JFRDaemon", e);
        }

        if (!initSucceded) {
            LOGGER.error("Unable to obtain JFR connection, this may indicate that your JVM does not have JFR enabled. JFR profiling system will shutdown");
        }
        return initSucceded;
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

    public Profiler getProfiler() {
        return profiler;
    }
}
