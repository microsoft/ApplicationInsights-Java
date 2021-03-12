package com.microsoft.applicationinsights.profiler;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import com.microsoft.applicationinsights.profileUploader.UploadCompleteHandler;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import org.apache.http.impl.client.CloseableHttpClient;

public interface ProfilerServiceFactory {

    Future<ProfilerService> initialize(
            Supplier<String> appIdSupplier,
            UploadCompleteHandler uploadCompleteObserver,
            ProfilerConfigurationHandler profilerConfigurationHandler,
            String processId,
            ServiceProfilerServiceConfig config,
            String machineName,
            String instrumentationKey,
            CloseableHttpClient httpClient,
            ScheduledExecutorService serviceProfilerExecutorService
    );
}
