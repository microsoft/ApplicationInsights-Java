package com.microsoft.applicationinsights.channel;

import com.microsoft.applicationinsights.telemetry.Telemetry;

import java.util.concurrent.TimeUnit;

/**
 * Represents a communication channel for sending telemetry to application insights.
 */
public interface TelemetryChannel
{
    /**
     *  Gets value indicating whether this channel is in developer mode.
     */
    boolean isDeveloperMode();

    /**
     *  Sets value indicating whether this channel is in developer mode.
     * @param value True for applying develoer mode
     */
    void setDeveloperMode(boolean value);

    /**
     *  Sends a Telemetry instance through the channel.
     */
    void send(Telemetry item);

    /**
     * Stops on going work
     * @param timeout Time to try and stop
     * @param timeUnit The units of the 'timeout' parameter
     */
    void stop(long timeout, TimeUnit timeUnit);
}
