package com.microsoft.applicationinsights.logging.log4j.v1_2;

import java.util.HashMap;
import java.util.Map;
import com.microsoft.applicationinsights.logging.common.ApplicationInsightsEvent;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;

public class ApplicationInsightsLogEvent extends ApplicationInsightsEvent {

    private LoggingEvent loggingEvent;

    public ApplicationInsightsLogEvent(LoggingEvent event) {
        this.loggingEvent = event;
    }

    @Override
    public String getMessage() {
        String message = this.loggingEvent.getRenderedMessage();

        return message != null ? message : "Log4j Trace";
    }

    @Override
    public boolean isException() {
        return this.loggingEvent.getThrowableInformation() != null;
    }

    @Override
    public Exception getException() {
        Exception exception = null;

        if (isException()) {
            Throwable throwable = this.loggingEvent.getThrowableInformation().getThrowable();
            exception = throwable instanceof Exception ? (Exception) throwable : new Exception(throwable);
        }

        return exception;
    }

    @Override
    public Map<String, String> getCustomParameters() {
        Map<String, String> metaData = new HashMap<String, String>();

        metaData.put("SourceType", "Log4j");

        addLogEventProperty("LoggerName", loggingEvent.getLoggerName(), metaData);
        addLogEventProperty("LoggingLevel", loggingEvent.getLevel() != null ? loggingEvent.getLevel().toString() : null, metaData);
        addLogEventProperty("ThreadName", loggingEvent.getThreadName(), metaData);
        addLogEventProperty("TimeStamp", getFormattedDate(loggingEvent.getTimeStamp()), metaData);

        if (loggingEvent.locationInformationExists()) {
            LocationInfo locationInfo = loggingEvent.getLocationInformation();

            addLogEventProperty("ClassName", locationInfo.getClassName(), metaData);
            addLogEventProperty("FileName", locationInfo.getFileName(), metaData);
            addLogEventProperty("MethodName", locationInfo.getMethodName(), metaData);
            addLogEventProperty("LineNumber", String.valueOf(locationInfo.getLineNumber()), metaData);
        }

        // TODO: Username, domain and identity should be included as in .NET version.
        // TODO: Should check, seems that it is not included in Log4j2.

        return metaData;
    }
}
