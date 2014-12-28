package com.microsoft.applicationinsights.logging.logback;

import java.util.HashMap;
import java.util.Map;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.microsoft.applicationinsights.logging.common.ApplicationInsightsEvent;

public class ApplicationInsightsLogEvent extends ApplicationInsightsEvent {

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
        Map<String, String> metaData = new HashMap<>();

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
}