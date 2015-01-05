package com.microsoft.applicationinsights.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.microsoft.applicationinsights.common.LogTelemetryClientProxy;
import com.microsoft.applicationinsights.common.TelemetryClientProxy;

/**
 * LOGBack appender.
 */
public class ApplicationInsightsAppender extends AppenderBase<ILoggingEvent> {

    // region Members

    private LogTelemetryClientProxy logTelemetryClientProxy;
    private String instrumentationKey;

    // endregion Members

    // region Public methods

    public TelemetryClientProxy getTelemetryClientProxy() {
        return this.logTelemetryClientProxy;
    }

    /**
     * Sets the instrumentation key.
     *
     * @param instrumentationKey The instrumentation key.
     */
    public void setInstrumentationKey(String instrumentationKey) {
        this.instrumentationKey = instrumentationKey;
    }

    /**
     * Appends the new event.
     * Catching exceptions and check if the appender has been started is not necessary
     * as it all taken care by the AppenderBase class.
     *
     * @param eventObject The event to append.
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        ApplicationInsightsLogEvent aiEvent = new ApplicationInsightsLogEvent(eventObject);
        this.logTelemetryClientProxy.sendEvent(aiEvent);
    }

    @Override
    public void start() {
        super.start();

        this.logTelemetryClientProxy = new LogTelemetryClientProxy(this.instrumentationKey);
    }
}