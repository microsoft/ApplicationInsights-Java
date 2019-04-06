package com.microsoft.applicationinsights.internal.profile;

/**
 * Responsible for CDS Retry Policy configuration.
 */
public enum CdsRetryPolicy {
    /**
     * Cached instance to be reused across SDK for CDS Profile fetch calls.
     */
    INSTANCE;

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

    /**
     * Private Constructor that sets the default value of maxInstantRetries to 3
     * and default resetPeriodInMinutes to 240.
     */
    CdsRetryPolicy() {
        maxInstantRetries = 3;
        resetPeriodInMinutes = 240;
    }

    /**
     * Resets the CDS configuration policy to default.
     */
    /* Visible for Testing */
    void resetConfiguration() {
        INSTANCE.maxInstantRetries = 3;
        INSTANCE.resetPeriodInMinutes = 240;
    }
}

