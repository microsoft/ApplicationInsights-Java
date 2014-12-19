package com.microsoft.applicationinsights.channel;

public interface IChannelConfig {
    /**
     * @return The instrumentation key for this telemetryContext
     */
    public String getInstrumentationKey();

    /**
     * @return The sender instance for this channel
     */
    public SenderConfig getSenderConfig();
}