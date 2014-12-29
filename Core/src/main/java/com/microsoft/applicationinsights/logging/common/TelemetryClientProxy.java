package com.microsoft.applicationinsights.logging.common;

import com.microsoft.applicationinsights.channel.TelemetryClient;

public interface TelemetryClientProxy {

    /**
     * Sends the given event to AI.
     *
     * @param event Event to send.
     */
    void sendEvent(ApplicationInsightsEvent event);

    /**
     * Gets the telemetry client.
     */
    TelemetryClient getTelemetryClient();
}
