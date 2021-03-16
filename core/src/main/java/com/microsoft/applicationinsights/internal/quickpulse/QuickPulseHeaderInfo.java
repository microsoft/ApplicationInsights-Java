package com.microsoft.applicationinsights.internal.quickpulse;

public class QuickPulseHeaderInfo {

    private final QuickPulseStatus quickPulseStatus;
    private final String qpsServiceEndpointRedirect;
    private final long qpsServicePollingInterval;

    public QuickPulseHeaderInfo(QuickPulseStatus quickPulseStatus,
                                String qpsServiceEndpointRedirect, long qpsServicePollingIntervalHint) {

        this.quickPulseStatus = quickPulseStatus;
        this.qpsServiceEndpointRedirect = qpsServiceEndpointRedirect;
        this.qpsServicePollingInterval = qpsServicePollingIntervalHint;
    }

    public QuickPulseHeaderInfo(QuickPulseStatus quickPulseStatus) {
        this.quickPulseStatus = quickPulseStatus;
        this.qpsServiceEndpointRedirect = null;
        this.qpsServicePollingInterval = -1;
    }

    public long getQpsServicePollingInterval() {
        return qpsServicePollingInterval;
    }

    public String getQpsServiceEndpointRedirect() {
        return qpsServiceEndpointRedirect;
    }

    public QuickPulseStatus getQuickPulseStatus() {
        return quickPulseStatus;
    }
}
