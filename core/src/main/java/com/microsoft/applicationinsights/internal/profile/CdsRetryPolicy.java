package com.microsoft.applicationinsights.internal.profile;

/**
 * Responsible for CDS Retry Policy configuration.
 */
public class CdsRetryPolicy {

    public static final int DEFAULT_MAX_INSTANT_RETRIES = 3;
    public static final int DEFAULT_RESET_PERIOD_IN_MINUTES = 240;
    /**
     * Maximum number of instant retries to CDS to resolve ikey to AppId.
     */
    private int maxInstantRetries;

    /**
     * The interval in minutes for retry counters and pending tasks to be cleaned.
     */
    private long resetPeriodInMinutes;

    public int getMaxInstantRetries() {
        return maxInstantRetries;
    }

    public long getResetPeriodInMinutes() {
        return resetPeriodInMinutes;
    }

    public void setMaxInstantRetries(int maxInstantRetries) {
        if (maxInstantRetries < 1) {
            throw new IllegalArgumentException("CDS maxInstantRetries should be at least 1");
        }
        this.maxInstantRetries = maxInstantRetries;
    }

    public void setResetPeriodInMinutes(long resetPeriodInMinutes) {
        if (resetPeriodInMinutes < 1) {
            throw new IllegalArgumentException("CDS retries reset interval should be at least 1 minute");
        }
        this.resetPeriodInMinutes = resetPeriodInMinutes;
    }

    public CdsRetryPolicy() {
        maxInstantRetries = DEFAULT_MAX_INSTANT_RETRIES;
        resetPeriodInMinutes = DEFAULT_RESET_PERIOD_IN_MINUTES;
    }
}

