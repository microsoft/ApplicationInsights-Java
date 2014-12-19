package com.microsoft.applicationinsights.channel;

public interface IContextConfig extends IChannelConfig {
    public static int defaultSessionRenewalMs = 30 * 60 * 1000; // 30 minutes
    public static int defaultSessionExpirationMs = 24 * 60 * 60 * 1000; // 24 hours

    /**
     * @return The account id for this telemetryContext
     */
    public String getAccountId();

    /**
     * @return The number of milliseconds which must expire before a session is renewed.
     */
    public int getSessionRenewalMs();

    /**
     * @return The number of milliseconds until a session expires.
     */
    public int getSessionExpirationMs();
}
