/*
 * ApplicationInsights-Java
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

package com.microsoft.applicationinsights.logback.internal;

import java.util.HashMap;
import java.util.Map;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.microsoft.applicationinsights.internal.common.ApplicationInsightsEvent;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import ch.qos.logback.classic.Level;

public final class ApplicationInsightsLogEvent extends ApplicationInsightsEvent {

    private ILoggingEvent loggingEvent;

    public ApplicationInsightsLogEvent(ILoggingEvent loggingEvent) {
        this.loggingEvent = loggingEvent;
    }

    @Override
    public String getMessage() {
        return this.loggingEvent.getFormattedMessage();
    }

    @Override
    public boolean isException() {
        return this.loggingEvent.getThrowableProxy() != null;
    }

    @Override
    public Exception getException() {
        Exception exception = null;

        if (isException()) {
            Throwable throwable = ((ThrowableProxy)this.loggingEvent.getThrowableProxy()).getThrowable();
            exception = throwable instanceof Exception ? (Exception) throwable : new Exception(throwable);
        }

        return exception;
    }

    @Override
    public Map<String, String> getCustomParameters() {
        Map<String, String> metaData = new HashMap<String, String>();

        metaData.put("SourceType", "LOGBack");

        addLogEventProperty("LoggerName", loggingEvent.getLoggerName(), metaData);
        addLogEventProperty("LoggingLevel", loggingEvent.getLevel() != null ? loggingEvent.getLevel().levelStr : null, metaData);
        addLogEventProperty("ThreadName", loggingEvent.getThreadName(), metaData);
        addLogEventProperty("TimeStamp", getFormattedDate(loggingEvent.getTimeStamp()), metaData);

        // TODO: No location info?
        // TODO: Username, domain and identity should be included as in .NET version.
        // TODO: Should check, seems that it is not included in Log4j2.

        return metaData;
    }

    @Override
    public SeverityLevel getNormalizedSeverityLevel() {
        int log4jLevelAsInt = loggingEvent.getLevel().toInt();
        switch (log4jLevelAsInt) {
            case Level.ERROR_INT: // ERROR
                return SeverityLevel.Error;

            case Level.WARN_INT: // WARN
                return SeverityLevel.Warning;

            case Level.INFO_INT: // INFO
                return SeverityLevel.Information;

            case Level.TRACE_INT: // TRACE
            case Level.DEBUG_INT: // DEBUG
            case Level.ALL_INT:   // ALL
                return SeverityLevel.Verbose;

            default:
                InternalLogger.INSTANCE.error("Unknown Logback option, %d, using TRACE level as default", log4jLevelAsInt);
                return SeverityLevel.Verbose;
        }
    }
}