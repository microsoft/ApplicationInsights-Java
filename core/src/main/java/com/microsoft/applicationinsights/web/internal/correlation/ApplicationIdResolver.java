package com.microsoft.applicationinsights.web.internal.correlation;

import com.microsoft.applicationinsights.TelemetryConfiguration;

/**
 * Retrieves the application id from storage
 */
public interface ApplicationIdResolver {
    ProfileFetcherResult fetchApplicationId(String instrumentationKey, TelemetryConfiguration configuration) throws ApplicationIdResolutionException, InterruptedException;
}
