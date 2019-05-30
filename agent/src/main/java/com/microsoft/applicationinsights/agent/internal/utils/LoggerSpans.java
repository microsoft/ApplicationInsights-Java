package com.microsoft.applicationinsights.agent.internal.utils;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import com.microsoft.applicationinsights.telemetry.TraceTelemetry;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.xyzzy.instrumentation.api.MessageSupplier;
import org.glowroot.xyzzy.instrumentation.api.internal.ReadableMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class LoggerSpans {

    public static void track(MessageSupplier messageSupplier, @Nullable Throwable throwable, long timeMillis,
                             TelemetryClient client) {

        ReadableMessage message = (ReadableMessage) messageSupplier.get();
        String formattedMessage = message.getText();
        Map<String, ?> detail = message.getDetail();

        String level = (String) detail.get("Level");
        SeverityLevel severityLevel = toSeverityLevel(level);

        String loggerName = (String) detail.get("Logger name");

        Telemetry telemetry;
        if (throwable == null) {
            TraceTelemetry traceTelemetry = new TraceTelemetry(formattedMessage);
            traceTelemetry.setSeverityLevel(severityLevel);
            telemetry = traceTelemetry;
        } else {
            ExceptionTelemetry exceptionTelemetry = new ExceptionTelemetry(throwable);
            exceptionTelemetry.setSeverityLevel(severityLevel);
            telemetry = exceptionTelemetry;
        }

        Map<String, String> properties = telemetry.getContext().getProperties();

        // TODO SourceType? e.g. "Log4j", "LOGBack"
        properties.put("SourceType", "Logger");
        properties.put("TimeStamp", getFormattedDate(timeMillis));
        if (level != null) {
            properties.put("LoggingLevel", level);
        }
        if (loggerName != null) {
            properties.put("LoggerName", loggerName);
        }
        // TODO ThreadName

        if (throwable != null) {
            properties.put("Logger Message", formattedMessage);
        }

        // TODO log4j: location information? ClassName, FileName, MethodName, LineNumber

        // TODO log4j and logback custom log event properties?

        // TODO: Username, domain and identity should be included as in .NET version.
        // TODO: Should check, seems that it is not included in Log4j2.

        client.track(telemetry);
    }

    protected static String getFormattedDate(long dateInMilliseconds) {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).format(new Date(dateInMilliseconds));
    }

    private static SeverityLevel toSeverityLevel(@Nullable String level) {
        switch (level) {
            case "FATAL":
                return SeverityLevel.Critical;
            case "ERROR":
                return SeverityLevel.Error;
            case "WARN":
                return SeverityLevel.Warning;
            case "INFO":
                return SeverityLevel.Information;
            case "DEBUG":
            case "TRACE":
            case "ALL":
                return SeverityLevel.Verbose;
            default:
                InternalLogger.INSTANCE.error("Unexpected level %s, using TRACE level as default", level);
                return SeverityLevel.Verbose;
        }
    }
}
