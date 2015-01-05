package com.microsoft.applicationinsights.common;

import com.microsoft.applicationinsights.TelemetryClient;

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

    /**
     * Gets a value indicating whether the proxy has been initialized.
     * @return True if initialized, false otherwise.
     */
    boolean isInitialized();
}
