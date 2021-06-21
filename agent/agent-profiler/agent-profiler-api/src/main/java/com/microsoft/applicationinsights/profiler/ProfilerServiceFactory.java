package com.microsoft.applicationinsights.profiler;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import com.azure.core.http.HttpPipeline;
import com.microsoft.applicationinsights.profileUploader.UploadCompleteHandler;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;

public interface ProfilerServiceFactory {

    Future<ProfilerService> initialize(
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
            String roleName
    );
}
