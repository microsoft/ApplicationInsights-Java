package com.microsoft.applicationinsights.channel;

/**
 * Represents a communication channel for sending telemetry to application insights.
 */
public interface TelemetryChannel
{
    /**
     *  Gets a flag indicating whether this channel is in developer mode.
     */
    boolean getDeveloperMode();

    /**
     *  Sets a flag indicating whether this channel is in developer mode.
     */
    void setDeveloperMode(boolean value);

    /**
     *  Sends a Telemetry instance through the channel.
     */
    void send(Telemetry item);
}
