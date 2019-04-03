package com.microsoft.applicationinsights.internal.profile;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;

/**
 * Class Responsible for configuration of Cds Profile Fetch.
 */
public final class CdsProfileFetcherPolicy {

    /**
     * Maximum number of instant retries to CDS to resolve ikey to AppId.
     */
    public int maxInstantRetries;

    /**
     * The interval in minutes for retry counters and pending tasks to be cleaned.
     */
    public long cachePurgePeriodInMinutes;

    /**
     * Cached instance to be reused across SDK for CDS Profile fetch calls.
     */
    private static CdsProfileFetcherPolicy instance;

    public int getMaxInstantRetries() {
        return maxInstantRetries;
    }

    public long getCachePurgePeriodInMinutes() {
        return cachePurgePeriodInMinutes;
    }

    public void setMaxInstantRetries(int maxInstantRetries) {
        this.maxInstantRetries = maxInstantRetries;
    }

    public void setCachePurgePeriodInMinutes(long cachePurgePeriodInMinutes) {
        this.cachePurgePeriodInMinutes = cachePurgePeriodInMinutes;
    }

    /**
     * Private Constructor that sets the default value of maxInstantRetries to 3
     * and default cachePurgePeriodInMinutes to 240.
     */
    private CdsProfileFetcherPolicy() {
        maxInstantRetries = 3;
        cachePurgePeriodInMinutes = 240;
    }

    /**
     * Returns an instance of CdsProfileFetcherPolicy
     * @return instance of CdsProfileFetcherPolicy
     */
    public static CdsProfileFetcherPolicy getInstance() {
        if (instance == null) {
            instance = new CdsProfileFetcherPolicy();
        }
        return instance;
    }

    /**
     * Resets the CDS configuration policy to default.
     */
    public void resetConfiguration() {
        if (instance != null) {
            InternalLogger.INSTANCE.warn(String.format("Resetting instance of CdsProfileFetcherRetryConfiguration - maxInstantRetries, " +
                    "cachePurgePeriodInMinutes to %d, % d minutes",maxInstantRetries, cachePurgePeriodInMinutes));
            maxInstantRetries = 3;
            cachePurgePeriodInMinutes = 240;
        } else {
            InternalLogger.INSTANCE.warn("No instance of CdsProfileFetcherRetryConfiguration is created");
        }
    }
}
