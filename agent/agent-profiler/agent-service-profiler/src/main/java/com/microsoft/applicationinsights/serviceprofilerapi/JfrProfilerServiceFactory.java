package com.microsoft.applicationinsights.serviceprofilerapi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import com.microsoft.applicationinsights.profileUploader.UploadCompleteHandler;
import com.microsoft.applicationinsights.profiler.ProfilerConfigurationHandler;
import com.microsoft.applicationinsights.profiler.ProfilerService;
import com.microsoft.applicationinsights.profiler.ProfilerServiceFactory;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ProfilerFrontendClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.profiler.JfrProfiler;
import com.microsoft.applicationinsights.serviceprofilerapi.upload.ServiceProfilerUploader;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Default ProfilerService factory loaded by a service loader, produces a Profiler Service based on JFR
 */
public class JfrProfilerServiceFactory implements ProfilerServiceFactory {
    // Singleton instance that holds the one and only service of the ServiceProfiler subsystem
    private static JfrProfilerService INSTANCE;

    @Override
    public synchronized Future<ProfilerService> initialize(
            Supplier<String> appIdSupplier,
            UploadCompleteHandler uploadCompleteObserver,
            ProfilerConfigurationHandler profilerConfigurationHandler,
            String processId,
            ServiceProfilerServiceConfig config,
            String machineName,
            String instrumentationKey,
            CloseableHttpClient httpClient,
            ScheduledExecutorService serviceProfilerExecutorService,
            String userAgent) {
        if (INSTANCE == null) {
            ServiceProfilerClientV2 serviceProfilerClient = new ProfilerFrontendClientV2(config.getServiceProfilerFrontEndPoint(), instrumentationKey, httpClient, userAgent);

            ServiceProfilerUploader uploader = new ServiceProfilerUploader(
                    serviceProfilerClient,
                    machineName,
                    processId,
                    appIdSupplier);

            INSTANCE = new JfrProfilerService(
                    appIdSupplier,
                    config,
                    new JfrProfiler(config),
                    profilerConfigurationHandler,
                    uploadCompleteObserver,
                    serviceProfilerClient,
                    uploader,
                    serviceProfilerExecutorService);

            return INSTANCE.initialize();
        }
        return CompletableFuture.completedFuture(INSTANCE);
    }

}
