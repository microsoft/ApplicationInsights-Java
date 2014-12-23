package com.microsoft.applicationinsights;

import com.microsoft.applicationinsights.channel.IChannelConfig;
import com.microsoft.applicationinsights.channel.IContextConfig;
import com.microsoft.applicationinsights.channel.Sender;
import com.microsoft.applicationinsights.channel.SenderConfig;

/**
 * Configuration object when instantiating TelemetryClient
 */
public class CoreTelemetryClientConfig implements IChannelConfig, IContextConfig {

    /**
     * The instrumentation key for this telemetryContext
     */
    protected final String instrumentationKey;

    /**
     * The account id for this telemetryContext
     */
    private String accountId;

    /**
     * The number of milliseconds which must expire before a session is renewed.
     */
    private int sessionRenewalMs;

    /**
     * The number of milliseconds until a session expires.
     */
    private int sessionExpirationMs;

    /**
     * The instrumentation key for this telemetryContext
     */
    public String getInstrumentationKey() {
        return this.instrumentationKey;
    }

    /**
     * The account id for this telemetryContext
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * The number of milliseconds which must expire before a session is renewed.
     */
    public int getSessionRenewalMs() {
        return sessionRenewalMs;
    }

    /**
     * The number of milliseconds until a session expires.
     */
    public int getSessionExpirationMs() {
        return sessionExpirationMs;
    }

    /**
     * @return The sender instance for this channel
     */
    public SenderConfig getSenderConfig() {
        return Sender.instance.getConfig();
    }

    /**
     * Constructs a new instance of the TelemetryClientConfig
     * @param iKey The instrumentation key
     */
    public CoreTelemetryClientConfig(String iKey){
        this(iKey, null);
    }

    /**
     * Constructs a new instance of the TelemetryClientConfig
     * @param iKey The instrumentation key
     * @param accountId The account ID
     */
    public CoreTelemetryClientConfig(String iKey, String accountId){
        this.accountId = accountId;
        this.instrumentationKey = iKey;
        this.sessionExpirationMs = IContextConfig.defaultSessionExpirationMs;
        this.sessionRenewalMs = IContextConfig.defaultSessionRenewalMs;
    }
}
