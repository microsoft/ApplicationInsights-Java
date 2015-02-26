/*
 * AppInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.microsoft.applicationinsights.internal.common.LogTelemetryClientProxy;
import com.microsoft.applicationinsights.internal.common.TelemetryClientProxy;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.logback.internal.ApplicationInsightsLogEvent;

/**
 * LOGBack appender.
 */
public class ApplicationInsightsAppender extends AppenderBase<ILoggingEvent> {

    // region Members

    private boolean isInitialized = false;
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
        if (!this.isStarted() || !this.isInitialized) {

            // TODO: trace not started or not initialized.
            return;
        }

        ApplicationInsightsLogEvent aiEvent = new ApplicationInsightsLogEvent(eventObject);
        this.logTelemetryClientProxy.sendEvent(aiEvent);
    }

    @Override
    public void start() {
        super.start();

        try {
            logTelemetryClientProxy = new LogTelemetryClientProxy(instrumentationKey);
            this.isInitialized = true;
        } catch (Exception e) {
            // Appender failure must not fail the running application.
            this.isInitialized = false;
            InternalLogger.INSTANCE.error("Failed to initialize appender with exception: %s.", e.getMessage());
        }
    }
}