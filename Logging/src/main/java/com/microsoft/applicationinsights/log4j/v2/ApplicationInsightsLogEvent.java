package com.microsoft.applicationinsights.log4j.v2;

import java.util.HashMap;
import java.util.Map;
import com.microsoft.applicationinsights.common.ApplicationInsightsEvent;
import org.apache.logging.log4j.core.LogEvent;

public class ApplicationInsightsLogEvent extends ApplicationInsightsEvent {

    private LogEvent logEvent;

    public ApplicationInsightsLogEvent(LogEvent logEvent) {
        this.logEvent = logEvent;
    }

    @Override
    public String getMessage() {
        String message = this.logEvent.getMessage() != null ?
                this.logEvent.getMessage().getFormattedMessage() :
                "Log4j Trace";

        return message;
    }

    @Override
    public boolean isException() {
        return this.logEvent.getThrown() != null;
    }

    @Override
    public Exception getException() {
        Exception exception = null;

        if (isException()) {
            Throwable throwable = this.logEvent.getThrown();
            exception = throwable instanceof Exception ? (Exception) throwable : new Exception(throwable);
        }

        return exception;
    }

    @Override
    public Map<String, String> getCustomParameters() {

        Map<String, String> metaData = new HashMap<String, String>();

        metaData.put("SourceType", "Log4j");

        addLogEventProperty("LoggerName", logEvent.getLoggerName(), metaData);
        addLogEventProperty("LoggingLevel", logEvent.getLevel() != null ? logEvent.getLevel().name() : null, metaData);
        addLogEventProperty("ThreadName", logEvent.getThreadName(), metaData);
        addLogEventProperty("TimeStamp", getFormattedDate(logEvent.getTimeMillis()), metaData);

        if (logEvent.isIncludeLocation()) {
            StackTraceElement stackTraceElement = logEvent.getSource();

            addLogEventProperty("ClassName", stackTraceElement.getClassName(), metaData);
            addLogEventProperty("FileName", stackTraceElement.getFileName(), metaData);
            addLogEventProperty("MethodName", stackTraceElement.getMethodName(), metaData);
            addLogEventProperty("LineNumber", String.valueOf(stackTraceElement.getLineNumber()), metaData);
        }

        // TODO: Username, domain and identity should be included as in .NET version.
        // TODO: Should check, seems that it is not included in Log4j2.

        return metaData;
    }
}
