package com.microsoft.applicationinsights.channel;

public class SenderConfig {
    /**
     * The url to which payloads will be sent
     */
    private String endpointUrl = "https://dc.services.visualstudio.com/v2/track";

    /**
     * The maximum size of a batch in bytes
     */
    private int maxBatchCount = 100;

    /**
     * The maximum interval allowed between calls to batchInvoke
     */
    private int maxBatchIntervalMs = 15 * 1000; // 15 seconds

    /**
     * The master off switch.  Do not send any data if set to TRUE
     */
    private boolean DisableTelemetry = false;

    /**
     * The platform specific internal logging mechanism
     */
    private ILoggingInternal internalLogger;

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public int getMaxBatchCount() {
        return maxBatchCount;
    }

    public void setMaxBatchCount(int maxBatchCount) {
        this.maxBatchCount = maxBatchCount;
    }

    public int getMaxBatchIntervalMs() {
        return maxBatchIntervalMs;
    }

    public void setMaxBatchIntervalMs(int maxBatchIntervalMs) {
        this.maxBatchIntervalMs = maxBatchIntervalMs;
    }

    public boolean isDisableTelemetry() {
        return DisableTelemetry;
    }

    public void setDisableTelemetry(boolean disableTelemetry) {
        DisableTelemetry = disableTelemetry;
    }

    public ILoggingInternal getLogger() {
        return internalLogger;
    }

    public void setLogger(ILoggingInternal logger) {
        this.internalLogger = logger;
    }
}