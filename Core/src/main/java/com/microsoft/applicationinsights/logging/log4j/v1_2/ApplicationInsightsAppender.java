package com.microsoft.applicationinsights.logging.log4j.v1_2;

import com.microsoft.applicationinsights.logging.common.TelemetryManager;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class ApplicationInsightsAppender extends AppenderSkeleton {

    // region Members

    private String instrumentationKey;
    private TelemetryManager telemetryManager;

    // endregion Members

    // region Public methods

    public TelemetryManager getTelemetryManager() {
        return this.telemetryManager;
    }

    /**
     * DO NOT REMOVE!
     * This method is used by Log4j system initializer when reading configuration.
     *
     * Sets the instrumentation key.
     *
     * @param key The instrumentation key.
     */
    public void setInstrumentationKey(String key) {
        this.instrumentationKey = key;
    }

    /**
     * Subclasses of <code>AppenderSkeleton</code> should implement this
     * method to perform actual logging. See also {@link #doAppend
     * AppenderSkeleton.doAppend} method.
     */
    @Override
    protected void append(LoggingEvent event) {
        if (this.closed) {

            // TODO: trace that closed.
            return;
        }

        try {
            ApplicationInsightsLogEvent aiEvent = new ApplicationInsightsLogEvent(event);
            this.telemetryManager.sendTelemetry(aiEvent);
        } catch (Exception e) {
            // Appender failure must not fail the running application.
            // TODO: Assert.Debug/warning on exception?
        }
    }

    /**
     * Release any allocated resources.
     */
    @Override
    public void close() {
        // No resources to release.
    }

    /**
     * This Appender converts the LoggingEvent it receives into a text string and
     * requires the layout format string to do so.
     */
    @Override
    public boolean requiresLayout() {
        return true;
    }

    /**
     * This method is being called on object initialization.
     */
    @Override
    public void activateOptions() {
        super.activateOptions();

        try {
            this.telemetryManager = new TelemetryManager(this.instrumentationKey);
        } catch (Exception e) {
            // Appender failure must not fail the running application.
            // TODO: Assert.Debug/warning on exception?
        }
    }

    // endregion Public methods
}
