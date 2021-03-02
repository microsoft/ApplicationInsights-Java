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
package com.microsoft.applicationinsights.internal.profiler;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import javax.management.InstanceNotFoundException;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.alerting.AlertingSubsystem;
import com.microsoft.applicationinsights.extensibility.initializer.TelemetryObservers;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.microsoft.applicationinsights.profileUploader.UploadCompleteHandler;
import com.microsoft.applicationinsights.profiler.ProfileHandler;
import com.microsoft.applicationinsights.profiler.Profiler;
import com.microsoft.applicationinsights.profiler.ProfilerConfigurationHandler;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ProfilerFrontendClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.config.ServiceProfilerConfigMonitorService;
import com.microsoft.applicationinsights.serviceprofilerapi.config.ServiceProfilerServiceConfig;
import com.microsoft.applicationinsights.serviceprofilerapi.upload.ServiceProfilerUploader;
import com.microsoft.applicationinsights.serviceprofilerapi.profiler.JFRService;
import com.microsoft.applicationinsights.serviceprofilerapi.profiler.JfrUploadService;
import com.microsoft.applicationinsights.serviceprofilerapi.config.AlertConfigParser;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service profiler main entry point, wires up:
 * - Configuration polling
 * - Alerting telemetry monitor subsystem
 * - JFR Profiling service
 * - JFR Uploader service
 */
public class ProfilerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilerService.class);

    private static final String APP_ID_PREFIX = "cid-v1:";


    private final ServiceProfilerServiceConfig config;
    private final ServiceProfilerClientV2 serviceProfilerClient;
    private final ServiceProfilerUploader serviceProfilerUploader;

    private final Supplier<String> appIdSupplier;
    private final TelemetryClient telemetryClient;

    @SuppressWarnings("unused")
    private final Profiler profiler;
    private ScheduledExecutorService serviceProfilerExecutorService;
    private ScheduledExecutorService alertServiceExecutorService;

    private boolean initialised = false;

    // Singleton instance that holds the one and only service of the ServiceProfiler subsystem
    private static ProfilerService INSTANCE;
    private ProfileHandler profileHandler;

    public synchronized static ProfilerService initialize(Supplier<String> appIdSupplier,
                                                          String processId,
                                                          ServiceProfilerServiceConfig config,
                                                          String machineName,
                                                          String instrumentationKey,
                                                          TelemetryClient client) {
        if (INSTANCE == null) {
            appIdSupplier = getAppId(appIdSupplier);
            ServiceProfilerClientV2 serviceProfilerClient = new ProfilerFrontendClientV2(config.getServiceProfilerFrontEndPoint(), instrumentationKey);

            ServiceProfilerUploader uploader = new ServiceProfilerUploader(
                    serviceProfilerClient,
                    machineName,
                    processId,
                    appIdSupplier);

            INSTANCE = new ProfilerService(
                    appIdSupplier,
                    config,
                    new JFRService(config),
                    serviceProfilerClient,
                    uploader,
                    client);
            INSTANCE.initialize();
        }

        return INSTANCE;
    }

    public ProfilerService(Supplier<String> appIdSupplier,
                           ServiceProfilerServiceConfig config,
                           Profiler profiler,
                           ServiceProfilerClientV2 serviceProfilerClient,
                           ServiceProfilerUploader serviceProfilerUploader,
                           TelemetryClient telemetryClient
    ) {
        this.appIdSupplier = getAppId(appIdSupplier);
        this.config = config;
        this.profiler = profiler;
        this.serviceProfilerClient = serviceProfilerClient;
        this.serviceProfilerUploader = serviceProfilerUploader;
        this.telemetryClient = telemetryClient;
    }

    public void initialize() {
        if (initialised || !config.enabled()) {
            return;
        }

        initialised = true;

        appIdSupplier.get();

        serviceProfilerExecutorService = Executors.newScheduledThreadPool(2,
                ThreadPoolUtils.createDaemonThreadFactory(ServiceProfilerConfigMonitorService.class, "ServiceProfilerService")
        );

        alertServiceExecutorService = Executors.newSingleThreadScheduledExecutor(
                ThreadPoolUtils.createDaemonThreadFactory(ServiceProfilerConfigMonitorService.class, "ServiceProfilerAlertingService")
        );

        profileHandler = new JfrUploadService(serviceProfilerUploader, appIdSupplier, sendServiceProfilerIndex());

        serviceProfilerExecutorService.submit(() -> {
            try {
                if (!initialiseProfiler()) return;

                AlertingSubsystem alertingSubsystem = createAlertMonitor(profiler);

                // Monitor service remains alive permanently due to scheduling an periodic config pull
                ServiceProfilerConfigMonitorService
                        .createServiceProfilerConfigService(
                                serviceProfilerExecutorService,
                                serviceProfilerClient,
                                Arrays.asList(updateAlertingConfig(alertingSubsystem), profiler),
                                config);
            } catch (Exception e) {
                LOGGER.error("Failed to initialise alert service", e);
            } catch (Error e) {
                LOGGER.error("Failed to initialise alert service", e);
                throw e;
            }
        });
    }

    private ProfilerConfigurationHandler updateAlertingConfig(AlertingSubsystem alertingSubsystem) {
        return config -> alertingSubsystem.updateConfiguration(AlertConfigParser.toAlertingConfig(config));
    }

    private boolean initialiseProfiler() {
        boolean initFailed = false;
        try {
            // Daemon remains alive permanently due to scheduling an update
            initFailed = profiler.initialize(profileHandler, serviceProfilerExecutorService);
        } catch (IOException | InstanceNotFoundException e) {
            LOGGER.error("Could not initialize JFRDaemon", e);
        }

        if (!initFailed) {
            LOGGER.error("Unable to obtain JFR connection, this may indicate that your JVM does not have JFR enabled. JFR profiling system will shutdown");
            alertServiceExecutorService.shutdown();
            serviceProfilerExecutorService.shutdown();
            return false;
        }
        return true;
    }

    private UploadCompleteHandler sendServiceProfilerIndex() {
        return done -> {
            EventTelemetry event = new EventTelemetry("ServiceProfilerIndex");
            event.getProperties().putAll(done.getServiceProfilerIndex().getProperties());
            event.getMetrics().putAll(done.getServiceProfilerIndex().getMetrics());
            telemetryClient.track(event);
        };
    }

    private AlertingSubsystem createAlertMonitor(Profiler profiler) {
        return AlertingServiceFactory.create(profiler, TelemetryObservers.INSTANCE, alertServiceExecutorService);
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
}
