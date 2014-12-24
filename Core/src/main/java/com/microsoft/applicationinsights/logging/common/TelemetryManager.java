package com.microsoft.applicationinsights.logging.common;

import java.util.Map;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.datacontracts.BaseTelemetry;
import com.microsoft.applicationinsights.datacontracts.ExceptionTelemetry;
import com.microsoft.applicationinsights.datacontracts.TraceTelemetry;
import com.microsoft.applicationinsights.util.StringUtil;

/**
 * This class encapsulates all the common logic for sending AI telemetry.
 * This class is used by all Appenders, Listeners etc.
 */
public class TelemetryManager {

    // region Members

    private TelemetryClient telemetryClient;

    // endregion Members

    // region Constructor

    /**
     * Constructs new Telemetry Manager instance.
     * @param instrumentationKey The instrumentation key.
     */
    public TelemetryManager(String instrumentationKey) {

        this.telemetryClient = new TelemetryClient();
        if (!StringUtil.isNullOrEmpty(instrumentationKey)) {
            this.telemetryClient.getContext().setInstrumentationKey(instrumentationKey);
        }
    }

    // endregion Constructor

    // region Public methods

    /**
     * Sends the given telemetry to AI.
     *
     * @param event
     */
    public void sendTelemetry(ApplicationInsightsEvent event) {

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
