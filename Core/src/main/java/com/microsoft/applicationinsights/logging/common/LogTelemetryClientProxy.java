package com.microsoft.applicationinsights.logging.common;

import java.util.Map;

import com.microsoft.applicationinsights.channel.TelemetryClient;
import com.microsoft.applicationinsights.datacontracts.BaseTelemetry;
import com.microsoft.applicationinsights.datacontracts.ExceptionTelemetry;
import com.microsoft.applicationinsights.datacontracts.TraceTelemetry;
import com.microsoft.applicationinsights.util.DefaultTelemetryClient;
import com.microsoft.applicationinsights.util.LocalStringsUtils;

/**
 * This class encapsulates all the common logic for sending AI telemetry.
 * This class is used by all Appenders, Listeners etc and therefore keeping them without
 * any logic.
 */
public class LogTelemetryClientProxy implements TelemetryClientProxy {

    // region Members

    private TelemetryClient telemetryClient;

    // endregion Members

    // region Constructor

    /**
     * Constructs new AI event sender instance.
     * @param instrumentationKey The instrumentation key for sending the events.
     */
    public LogTelemetryClientProxy(String instrumentationKey) {

        this.telemetryClient = new DefaultTelemetryClient();
        if (!LocalStringsUtils.isNullOrEmpty(instrumentationKey)) {
            this.telemetryClient.getContext().setInstrumentationKey(instrumentationKey);
        }
    }

    // endregion Constructor

    // region Public methods

    /**
     * Sends the given event to AI.
     *
     * @param event
     */
    public void sendEvent(ApplicationInsightsEvent event) {

        String formattedMessage = event.getMessage();

        BaseTelemetry telemetry = event.isException() ?
                new ExceptionTelemetry(event.getException()) :
                new TraceTelemetry(formattedMessage);

        Map<String, String> customParameters = event.getCustomParameters();
        telemetry.getContext().getProperties().putAll(customParameters);

        telemetryClient.track(telemetry);
    }

    /**
     * Gets the telemetry client.
     *
     * @return Telemetry client
     */
    public TelemetryClient getTelemetryClient() {
        return this.telemetryClient;
    }

    // endregion Public methods
}
