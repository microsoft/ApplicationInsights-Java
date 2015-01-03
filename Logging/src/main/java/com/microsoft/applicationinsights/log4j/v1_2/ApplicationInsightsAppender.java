package com.microsoft.applicationinsights.log4j.v1_2;

import com.microsoft.applicationinsights.common.LogTelemetryClientProxy;
import com.microsoft.applicationinsights.common.TelemetryClientProxy;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

public class ApplicationInsightsAppender extends AppenderSkeleton {

    // region Members

    private boolean isInitialized = true;
    private String instrumentationKey;
    private TelemetryClientProxy telemetryClientProxy;

    // endregion Members

    // region Public methods

    public TelemetryClientProxy getTelemetryClientProxy() {
        return this.telemetryClientProxy;
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
        if (this.closed || !this.isInitialized) {

            // TODO: trace that closed or not initialized.
            return;
        }

        try {
            ApplicationInsightsLogEvent aiEvent = new ApplicationInsightsLogEvent(event);
            this.telemetryClientProxy.sendEvent(aiEvent);
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
            this.telemetryClientProxy = new LogTelemetryClientProxy(this.instrumentationKey);
        } catch (Exception e) {
            // Appender failure must not fail the running application.
            // TODO: Assert.Debug/warning on exception?
            this.isInitialized = false;
        }
    }

    // endregion Public methods
}
