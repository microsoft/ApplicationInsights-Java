package com.microsoft.applicationinsights.web.internal.correlation;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.customExceptions.FriendlyException;

/**
 * Retrieves the application id from storage
 */
public interface ApplicationIdResolver {
    ProfileFetcherResult fetchApplicationId(String instrumentationKey, TelemetryConfiguration configuration) throws ApplicationIdResolutionException, InterruptedException, FriendlyException;
}
